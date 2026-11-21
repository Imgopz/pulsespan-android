package com.pulsespan.app.data.demo

import com.pulsespan.app.domain.model.DailyBiometrics
import com.pulsespan.app.domain.model.SleepSummary
import com.pulsespan.app.domain.model.WorkoutSummary
import java.time.LocalDate

/**
 * Provides realistic 14-day Google Fitbit biometric data so PulseSpan can be previewed
 * and tested immediately on an emulator or device without a paired tracker.
 */
object DemoFitbitDataProvider {

    fun generateLast14Days(today: LocalDate = LocalDate.now()): List<DailyBiometrics> {
        val samplePatterns = listOf(
            // (sleepMin, deep, rem, awake, hrv, rhr, steps, activeCal, elevHr, workoutTitle, workoutMin)
            Pattern(445, 88, 96, 32, 58.0, 59, 8920, 520, 38, "Zone 2 Outdoor Run", 40),
            Pattern(465, 95, 102, 26, 63.5, 57, 10450, 640, 52, "Upper Body Strength", 50),
            Pattern(410, 74, 85, 40, 54.0, 61, 7200, 410, 26, null, 0),
            Pattern(485, 104, 112, 22, 68.0, 56, 11800, 780, 64, "Tempo Run", 48),
            Pattern(390, 65, 78, 48, 49.5, 62, 6100, 320, 18, "Recovery Walk", 30),
            Pattern(455, 92, 98, 28, 61.0, 58, 9400, 590, 45, "Full Body Hypertrophy", 55),
            Pattern(470, 98, 105, 25, 65.0, 57, 12400, 810, 68, "Cycling Intervals", 52),
            Pattern(430, 80, 92, 35, 56.5, 60, 8100, 460, 32, "Core & Mobility", 25),
            Pattern(490, 108, 115, 20, 71.0, 55, 10900, 690, 56, "Zone 2 Run", 45),
            Pattern(365, 58, 72, 55, 45.0, 64, 5400, 290, 14, null, 0),
            Pattern(460, 94, 104, 28, 62.0, 58, 9800, 610, 48, "Lower Body Strength", 50),
            Pattern(475, 100, 108, 24, 66.5, 56, 11200, 720, 60, "HIIT Circuit", 35),
            Pattern(440, 86, 94, 30, 59.5, 58, 8650, 510, 36, "Evening Walk", 35),
            // Today: strong recovery & prime readiness
            Pattern(476, 102, 110, 22, 69.2, 55, 9640, 645, 51, "Zone 2 Morning Run", 42)
        )

        return samplePatterns.mapIndexed { index, p ->
            val date = today.minusDays((samplePatterns.size - 1 - index).toLong())
            val light = (p.sleepMin - p.deep - p.rem).coerceAtLeast(0)
            val workouts = if (p.workoutTitle != null) {
                listOf(
                    WorkoutSummary(
                        id = "demo_workout_$index",
                        title = p.workoutTitle,
                        durationMinutes = p.workoutMin,
                        activeCalories = (p.activeCal * 0.65).toInt(),
                        avgHeartRateBpm = 138 + (index % 14)
                    )
                )
            } else {
                emptyList()
            }

            DailyBiometrics(
                date = date,
                sleep = SleepSummary(
                    totalMinutes = p.sleepMin,
                    deepMinutes = p.deep,
                    remMinutes = p.rem,
                    lightMinutes = light,
                    awakeMinutes = p.awake
                ),
                hrvRmssdMs = p.hrv,
                restingHeartRateBpm = p.rhr,
                steps = p.steps,
                activeCalories = p.activeCal,
                totalCalories = 1750 + p.activeCal,
                elevatedHrMinutes = p.elevHr,
                workouts = workouts
            )
        }
    }

    private data class Pattern(
        val sleepMin: Int,
        val deep: Int,
        val rem: Int,
        val awake: Int,
        val hrv: Double,
        val rhr: Int,
        val steps: Int,
        val activeCal: Int,
        val elevHr: Int,
        val workoutTitle: String?,
        val workoutMin: Int
    )
}
