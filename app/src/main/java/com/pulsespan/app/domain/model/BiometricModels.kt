package com.pulsespan.app.domain.model

import java.time.LocalDate

enum class ReadinessZone(val label: String, val guidance: String) {
    PRIME("Prime", "Body is primed for high-intensity training or peak performance."),
    BALANCED("Balanced", "Good readiness for steady aerobic work or moderate strength."),
    RESTORE("Restore", "Prioritize sleep, hydration, mobility, and low-intensity recovery.")
}

data class SleepSummary(
    val totalMinutes: Int,
    val deepMinutes: Int,
    val remMinutes: Int,
    val lightMinutes: Int,
    val awakeMinutes: Int
) {
    val totalInBedMinutes: Int get() = totalMinutes + awakeMinutes
    val efficiencyPercent: Int
        get() = if (totalInBedMinutes > 0) ((totalMinutes * 100.0) / totalInBedMinutes).toInt().coerceIn(0, 100) else 0

    val deepPercent: Int
        get() = if (totalMinutes > 0) ((deepMinutes * 100.0) / totalMinutes).toInt() else 0

    val remPercent: Int
        get() = if (totalMinutes > 0) ((remMinutes * 100.0) / totalMinutes).toInt() else 0

    val formattedDuration: String
        get() {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return "${hours}h ${mins}m"
        }
}

data class WorkoutSummary(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val activeCalories: Int,
    val avgHeartRateBpm: Int? = null
)

data class DailyBiometrics(
    val date: LocalDate,
    val sleep: SleepSummary,
    val hrvRmssdMs: Double?,
    val restingHeartRateBpm: Int?,
    val steps: Int,
    val activeCalories: Int,
    val totalCalories: Int,
    val elevatedHrMinutes: Int,
    val workouts: List<WorkoutSummary> = emptyList()
)

data class PersonalBaseline(
    val avgHrvRmssdMs: Double,
    val avgRestingHeartRateBpm: Double,
    val targetSleepMinutes: Int = 480 // 8h default
)

data class HealthScores(
    val date: LocalDate,
    val unifiedLongevityScore: Int,
    val sleepScore: Int,
    val recoveryScore: Int,
    val strainScore: Int,
    val readinessZone: ReadinessZone,
    val hrvDeltaPercent: Int?,
    val rhrDeltaBpm: Int?
)

data class DailySnapshot(
    val biometrics: DailyBiometrics,
    val scores: HealthScores
)

data class CoachMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED
}
