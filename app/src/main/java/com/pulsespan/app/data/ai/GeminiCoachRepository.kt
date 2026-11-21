package com.pulsespan.app.data.ai

import com.pulsespan.app.BuildConfig
import com.pulsespan.app.domain.model.DailySnapshot
import com.pulsespan.app.domain.model.PersonalBaseline
import com.pulsespan.app.domain.model.ReadinessZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface CoachRepository {
    suspend fun generateDailyBriefing(today: DailySnapshot, baseline: PersonalBaseline): String
    suspend fun askCoach(question: String, today: DailySnapshot, baseline: PersonalBaseline): String
}

/**
 * Gemini-powered longevity coach.
 *
 * Development: uses BuildConfig.GEMINI_API_KEY from local.properties.
 * Production (Google Play): swap this implementation for Firebase AI Logic or your own HTTPS proxy
 * so no API key is bundled in the release APK/AAB.
 */
class GeminiCoachRepository(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = BuildConfig.GEMINI_MODEL
) : CoachRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generateDailyBriefing(
        today: DailySnapshot,
        baseline: PersonalBaseline
    ): String {
        if (apiKey.isBlank()) {
            return buildLocalBriefing(today, baseline)
        }

        val prompt = """
            You are PulseSpan, an evidence-based longevity and fitness coach analyzing a user's Google Fitbit biometrics.
            Give a crisp 2-3 sentence daily readiness briefing with one concrete workout or recovery recommendation.
            Do not give medical diagnoses.

            Metrics today:
            - Unified Longevity Score: ${today.scores.unifiedLongevityScore}/100
            - Readiness Zone: ${today.scores.readinessZone.label} (Recovery ${today.scores.recoveryScore}/100)
            - Sleep Score: ${today.scores.sleepScore}/100 (${today.biometrics.sleep.formattedDuration}, Deep ${today.biometrics.sleep.deepPercent}%, REM ${today.biometrics.sleep.remPercent}%)
            - Overnight HRV: ${today.biometrics.hrvRmssdMs?.let { "%.1f ms".format(it) } ?: "N/A"} (14d baseline: ${"%.1f ms".format(baseline.avgHrvRmssdMs)})
            - Resting Heart Rate: ${today.biometrics.restingHeartRateBpm ?: "N/A"} bpm (baseline: ${"%.0f".format(baseline.avgRestingHeartRateBpm)} bpm)
            - Strain Score: ${today.scores.strainScore}/100 (${today.biometrics.steps} steps, ${today.biometrics.activeCalories} active kcal)
        """.trimIndent()

        return callGemini(prompt) ?: buildLocalBriefing(today, baseline)
    }

    override suspend fun askCoach(
        question: String,
        today: DailySnapshot,
        baseline: PersonalBaseline
    ): String {
        if (apiKey.isBlank()) {
            return buildLocalAnswer(question, today, baseline)
        }

        val prompt = """
            You are PulseSpan, a concise AI longevity coach grounded in the user's Fitbit data.
            Keep your answer actionable, friendly, and under 120 words. Avoid medical diagnosis.

            Current Fitbit Context:
            - Longevity Score: ${today.scores.unifiedLongevityScore}/100, Readiness: ${today.scores.readinessZone.label} (${today.scores.recoveryScore}/100)
            - Sleep: ${today.biometrics.sleep.formattedDuration} (Score ${today.scores.sleepScore}/100, Deep ${today.biometrics.sleep.deepPercent}%, REM ${today.biometrics.sleep.remPercent}%)
            - HRV: ${today.biometrics.hrvRmssdMs?.let { "%.1f ms".format(it) } ?: "N/A"} vs baseline ${"%.1f ms".format(baseline.avgHrvRmssdMs)}
            - RHR: ${today.biometrics.restingHeartRateBpm ?: "N/A"} bpm vs baseline ${"%.0f".format(baseline.avgRestingHeartRateBpm)} bpm
            - Strain: ${today.scores.strainScore}/100 (${today.biometrics.steps} steps, ${today.biometrics.activeCalories} active kcal)

            User Question: $question
        """.trimIndent()

        return callGemini(prompt) ?: buildLocalAnswer(question, today, baseline)
    }

    private suspend fun callGemini(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val payload = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )
            val body = json.encodeToString(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val raw = response.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<GeminiResponse>(raw)
                parsed.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildLocalBriefing(today: DailySnapshot, baseline: PersonalBaseline): String {
        val hrvText = today.scores.hrvDeltaPercent?.let { delta ->
            val sign = if (delta >= 0) "+" else ""
            "HRV is $sign$delta% vs. your 14-day baseline (${"%.0f".format(baseline.avgHrvRmssdMs)} ms)"
        } ?: "Connect Fitbit HRV in Health Connect for deeper recovery precision"

        return when (today.scores.readinessZone) {
            ReadinessZone.PRIME ->
                "You're in the Prime zone (${today.scores.recoveryScore}/100) after ${today.biometrics.sleep.formattedDuration} of sleep. $hrvText — today is ideal for a progressive strength session or Zone 3–4 cardio."
            ReadinessZone.BALANCED ->
                "Your readiness is Balanced (${today.scores.recoveryScore}/100). $hrvText. Aim for 30–45 minutes of Zone 2 aerobic work or moderate strength while keeping evening wind-down consistent."
            ReadinessZone.RESTORE ->
                "Your body is signaling Restore (${today.scores.recoveryScore}/100) with ${today.biometrics.sleep.formattedDuration} sleep. $hrvText. Swap high-intensity training for a recovery walk, mobility, and an earlier bedtime."
        }
    }

    private fun buildLocalAnswer(
        question: String,
        today: DailySnapshot,
        baseline: PersonalBaseline
    ): String {
        val lower = question.lowercase()
        return when {
            "workout" in lower || "train" in lower || "run" in lower ->
                "Based on your ${today.scores.readinessZone.label} readiness (${today.scores.recoveryScore}/100) and current strain (${today.scores.strainScore}/100), ${today.scores.readinessZone.guidance}"
            "sleep" in lower ->
                "You logged ${today.biometrics.sleep.formattedDuration} (Sleep Score ${today.scores.sleepScore}/100) with ${today.biometrics.sleep.deepPercent}% Deep and ${today.biometrics.sleep.remPercent}% REM. Keeping a consistent bedtime helps stabilize overnight HRV."
            "hrv" in lower || "recover" in lower ->
                "Today's HRV is ${today.biometrics.hrvRmssdMs?.let { "%.1f ms".format(it) } ?: "N/A"} compared to your 14-day baseline of ${"%.1f ms".format(baseline.avgHrvRmssdMs)}. Prioritize hydration, daylight exposure, and 8h sleep tonight."
            else ->
                buildLocalBriefing(today, baseline) + "\n\n(Tip: Add GEMINI_API_KEY in local.properties to enable live Gemini AI responses.)"
        }
    }

    @Serializable
    private data class GeminiRequest(val contents: List<GeminiContent>)

    @Serializable
    private data class GeminiContent(val parts: List<GeminiPart>)

    @Serializable
    private data class GeminiPart(val text: String)

    @Serializable
    private data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

    @Serializable
    private data class GeminiCandidate(val content: GeminiContent? = null)
}
