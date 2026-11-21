package com.pulsespan.app.domain.scoring

import com.pulsespan.app.domain.model.DailyBiometrics
import com.pulsespan.app.domain.model.HealthScores
import com.pulsespan.app.domain.model.PersonalBaseline
import com.pulsespan.app.domain.model.ReadinessZone
import com.pulsespan.app.domain.model.SleepSummary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Deterministic scoring engine inspired by Vora's Sleep, Recovery, Strain, and Longevity pillars.
 * All calculations are pure functions so they run locally on-device and are fully unit-testable.
 */
object HealthScoreEngine {

    private const val DEFAULT_HRV_MS = 55.0
    private const val DEFAULT_RHR_BPM = 62.0

    fun computeBaseline(history: List<DailyBiometrics>, targetSleepMinutes: Int = 480): PersonalBaseline {
        val hrvValues = history.mapNotNull { it.hrvRmssdMs }.filter { it > 0.0 }
        val rhrValues = history.mapNotNull { it.restingHeartRateBpm?.toDouble() }.filter { it > 0.0 }

        return PersonalBaseline(
            avgHrvRmssdMs = if (hrvValues.isNotEmpty()) hrvValues.average() else DEFAULT_HRV_MS,
            avgRestingHeartRateBpm = if (rhrValues.isNotEmpty()) rhrValues.average() else DEFAULT_RHR_BPM,
            targetSleepMinutes = targetSleepMinutes
        )
    }

    fun scoreDay(biometrics: DailyBiometrics, baseline: PersonalBaseline): HealthScores {
        val sleepScore = calculateSleepScore(biometrics.sleep, baseline.targetSleepMinutes)
        val recoveryScore = calculateRecoveryScore(biometrics, baseline, sleepScore)
        val strainScore = calculateStrainScore(biometrics)
        val unifiedScore = calculateUnifiedScore(sleepScore, recoveryScore, strainScore)

        val hrvDeltaPercent = biometrics.hrvRmssdMs?.let { hrv ->
            if (baseline.avgHrvRmssdMs > 0.0) {
                (((hrv - baseline.avgHrvRmssdMs) / baseline.avgHrvRmssdMs) * 100.0).roundToInt()
            } else null
        }

        val rhrDeltaBpm = biometrics.restingHeartRateBpm?.let { rhr ->
            (rhr - baseline.avgRestingHeartRateBpm).roundToInt()
        }

        return HealthScores(
            date = biometrics.date,
            unifiedLongevityScore = unifiedScore,
            sleepScore = sleepScore,
            recoveryScore = recoveryScore,
            strainScore = strainScore,
            readinessZone = determineReadinessZone(recoveryScore),
            hrvDeltaPercent = hrvDeltaPercent,
            rhrDeltaBpm = rhrDeltaBpm
        )
    }

    /**
     * Sleep Score (0..100):
     * - 50% Duration vs. target (default 8h)
     * - 30% Restorative sleep architecture (Deep + REM combined ideal ~40–45%)
     * - 20% Sleep efficiency (asleep / time in bed, ideal >= 90%)
     */
    fun calculateSleepScore(sleep: SleepSummary, targetMinutes: Int = 480): Int {
        if (sleep.totalMinutes <= 0) return 0

        val durationRatio = (sleep.totalMinutes.toDouble() / targetMinutes.coerceAtLeast(300))
            .coerceIn(0.0, 1.05)
        val durationPoints = (durationRatio.coerceAtMost(1.0) * 50.0)

        val restorativeRatio = (sleep.deepMinutes + sleep.remMinutes).toDouble() / sleep.totalMinutes
        // Ideal restorative fraction is around 0.42 (e.g., 20% deep + 22% REM)
        val restorativePoints = ((restorativeRatio / 0.42).coerceIn(0.0, 1.0)) * 30.0

        val efficiencyRatio = ((sleep.efficiencyPercent - 65.0) / 30.0).coerceIn(0.0, 1.0)
        val efficiencyPoints = efficiencyRatio * 20.0

        return (durationPoints + restorativePoints + efficiencyPoints)
            .roundToInt()
            .coerceIn(0, 100)
    }

    /**
     * Recovery / Readiness Score (0..100):
     * - 45% Overnight HRV relative to personal 14-day baseline
     * - 30% Resting Heart Rate relative to personal baseline (lower RHR = better recovery)
     * - 25% Sleep Score contribution
     */
    fun calculateRecoveryScore(
        biometrics: DailyBiometrics,
        baseline: PersonalBaseline,
        sleepScore: Int
    ): Int {
        val hrvComponent = biometrics.hrvRmssdMs?.let { hrv ->
            val ratio = hrv / baseline.avgHrvRmssdMs.coerceAtLeast(10.0)
            // Ratio 1.0 = 78/100 baseline; >=1.20 = 100; <=0.65 = 25
            val normalized = (78.0 + (ratio - 1.0) * 110.0).coerceIn(15.0, 100.0)
            normalized
        } ?: sleepScore.toDouble()

        val rhrComponent = biometrics.restingHeartRateBpm?.let { rhr ->
            val delta = rhr - baseline.avgRestingHeartRateBpm
            // Each bpm above baseline reduces score by ~5 pts; each bpm below adds ~4 pts
            val normalized = (80.0 - delta * 5.0).coerceIn(15.0, 100.0)
            normalized
        } ?: sleepScore.toDouble()

        val weighted = (hrvComponent * 0.45) + (rhrComponent * 0.30) + (sleepScore * 0.25)
        return weighted.roundToInt().coerceIn(0, 100)
    }

    /**
     * Strain Score (0..100):
     * Combines active calories, elevated HR minutes, steps, and logged workout volume.
     */
    fun calculateStrainScore(biometrics: DailyBiometrics): Int {
        val caloriePoints = (biometrics.activeCalories / 900.0).coerceIn(0.0, 1.0) * 40.0
        val hrZonePoints = (biometrics.elevatedHrMinutes / 75.0).coerceIn(0.0, 1.0) * 35.0
        val stepPoints = (biometrics.steps / 12000.0).coerceIn(0.0, 1.0) * 15.0
        val workoutMinutes = biometrics.workouts.sumOf { it.durationMinutes }
        val workoutPoints = (workoutMinutes / 60.0).coerceIn(0.0, 1.0) * 10.0

        return (caloriePoints + hrZonePoints + stepPoints + workoutPoints)
            .roundToInt()
            .coerceIn(0, 100)
    }

    /**
     * Unified Longevity Score (0..100):
     * Rewards strong sleep & recovery alongside healthy strain-to-readiness alignment
     * (avoiding chronic under-activity or overtraining relative to recovery).
     */
    fun calculateUnifiedScore(sleepScore: Int, recoveryScore: Int, strainScore: Int): Int {
        val optimalStrainTarget = (recoveryScore * 0.85).coerceIn(25.0, 90.0)
        val alignmentPenalty = (abs(strainScore - optimalStrainTarget) * 0.6).coerceAtMost(35.0)
        val strainBalanceScore = (100.0 - alignmentPenalty).coerceIn(0.0, 100.0)

        val unified = (recoveryScore * 0.45) + (sleepScore * 0.35) + (strainBalanceScore * 0.20)
        return unified.roundToInt().coerceIn(0, 100)
    }

    fun determineReadinessZone(recoveryScore: Int): ReadinessZone = when {
        recoveryScore >= 75 -> ReadinessZone.PRIME
        recoveryScore >= 50 -> ReadinessZone.BALANCED
        else -> ReadinessZone.RESTORE
    }
}
