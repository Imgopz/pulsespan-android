package com.pulsespan.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.pulsespan.app.domain.model.DailyBiometrics
import com.pulsespan.app.domain.model.HealthConnectAvailability
import com.pulsespan.app.domain.model.SleepSummary
import com.pulsespan.app.domain.model.WorkoutSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class HealthConnectManager(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun getAvailability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    private fun getClientOrNull(): HealthConnectClient? {
        return if (getAvailability() == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Reads the last [days] of Fitbit-synced metrics from Android Health Connect.
     */
    suspend fun readRecentBiometrics(days: Int = 14): List<DailyBiometrics> {
        val client = getClientOrNull() ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startDate = today.minusDays((days - 1).toLong())
        val startInstant = startDate.atStartOfDay(zone).toInstant()
        val endInstant = Instant.now()
        val range = TimeRangeFilter.between(startInstant, endInstant)

        val sleepRecords = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records
        val hrvRecords = client.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, range)).records
        val rhrRecords = client.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, range)).records
        val hrRecords = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, range)).records
        val stepRecords = client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records
        val activeCalRecords = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range)).records
        val totalCalRecords = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, range)).records
        val exerciseRecords = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records

        return (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dayStart = date.atStartOfDay(zone).toInstant()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

            // Sleep sessions that ended on `date` count toward that morning's readiness
            val daySleep = sleepRecords.filter { it.endTime >= dayStart && it.endTime < dayEnd }
            val sleepSummary = aggregateSleep(daySleep)

            val dayHrv = hrvRecords
                .filter { it.time >= dayStart && it.time < dayEnd }
                .map { it.heartRateVariabilityMillis }
                .takeIf { it.isNotEmpty() }
                ?.average()

            val dayRhr = rhrRecords
                .filter { it.time >= dayStart && it.time < dayEnd }
                .map { it.beatsPerMinute }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt()

            val daySteps = stepRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .sumOf { it.count }
                .toInt()

            val dayActiveCals = activeCalRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .sumOf { it.energy.inKilocalories }
                .roundToInt()

            val dayTotalCals = totalCalRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .sumOf { it.energy.inKilocalories }
                .roundToInt()
                .coerceAtLeast(dayActiveCals)

            // Approximate elevated HR minutes (samples >= 115 bpm)
            val elevatedSamples = hrRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .flatMap { it.samples }
                .count { it.beatsPerMinute >= 115L }
            val elevatedMinutes = (elevatedSamples / 2).coerceAtMost(240)

            val dayWorkouts = exerciseRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .mapIndexed { idx, session ->
                    val mins = Duration.between(session.startTime, session.endTime).toMinutes().toInt().coerceAtLeast(1)
                    WorkoutSummary(
                        id = session.metadata.id.ifBlank { "ex_${date}_$idx" },
                        title = session.title?.takeIf { it.isNotBlank() } ?: formatExerciseType(session.exerciseType),
                        durationMinutes = mins,
                        activeCalories = (mins * 7),
                        avgHeartRateBpm = null
                    )
                }

            DailyBiometrics(
                date = date,
                sleep = sleepSummary,
                hrvRmssdMs = dayHrv,
                restingHeartRateBpm = dayRhr,
                steps = daySteps,
                activeCalories = dayActiveCals,
                totalCalories = dayTotalCals,
                elevatedHrMinutes = elevatedMinutes,
                workouts = dayWorkouts
            )
        }
    }

    private fun aggregateSleep(sessions: List<SleepSessionRecord>): SleepSummary {
        if (sessions.isEmpty()) {
            return SleepSummary(0, 0, 0, 0, 0)
        }
        var deep = 0
        var rem = 0
        var light = 0
        var awake = 0
        var fallbackTotal = 0

        for (session in sessions) {
            if (session.stages.isEmpty()) {
                fallbackTotal += Duration.between(session.startTime, session.endTime).toMinutes().toInt()
            } else {
                for (stage in session.stages) {
                    val mins = Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> deep += mins
                        SleepSessionRecord.STAGE_TYPE_REM -> rem += mins
                        SleepSessionRecord.STAGE_TYPE_LIGHT,
                        SleepSessionRecord.STAGE_TYPE_SLEEPING -> light += mins
                        SleepSessionRecord.STAGE_TYPE_AWAKE,
                        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awake += mins
                    }
                }
            }
        }

        val totalAsleep = (deep + rem + light).let { if (it > 0) it else fallbackTotal }
        return SleepSummary(
            totalMinutes = totalAsleep,
            deepMinutes = deep,
            remMinutes = rem,
            lightMinutes = if (light > 0) light else (totalAsleep - deep - rem).coerceAtLeast(0),
            awakeMinutes = awake
        )
    }

    private fun formatExerciseType(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Run"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga & Mobility"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Swimming"
        else -> "Fitbit Workout"
    }

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
}
