package com.pulsespan.app.domain.scoring

import com.pulsespan.app.domain.model.DailyBiometrics
import com.pulsespan.app.domain.model.PersonalBaseline
import com.pulsespan.app.domain.model.ReadinessZone
import com.pulsespan.app.domain.model.SleepSummary
import com.pulsespan.app.domain.model.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HealthScoreEngineTest {

    private val baseline = PersonalBaseline(
        avgHrvRmssdMs = 60.0,
        avgRestingHeartRateBpm = 58.0,
        targetSleepMinutes = 480
    )

    @Test
    fun `optimal sleep and elevated HRV produces Prime readiness`() {
        val biometrics = DailyBiometrics(
            date = LocalDate.of(2026, 11, 21),
            sleep = SleepSummary(
                totalMinutes = 480,
                deepMinutes = 100,
                remMinutes = 110,
                lightMinutes = 270,
                awakeMinutes = 25
            ),
            hrvRmssdMs = 70.0,
            restingHeartRateBpm = 55,
            steps = 9500,
            activeCalories = 620,
            totalCalories = 2350,
            elevatedHrMinutes = 48,
            workouts = listOf(WorkoutSummary("w1", "Zone 2 Run", 45, 420, 144))
        )

        val scores = HealthScoreEngine.scoreDay(biometrics, baseline)

        assertTrue("Expected sleep score >= 90, got ${scores.sleepScore}", scores.sleepScore >= 90)
        assertTrue("Expected recovery score >= 85, got ${scores.recoveryScore}", scores.recoveryScore >= 85)
        assertEquals(ReadinessZone.PRIME, scores.readinessZone)
        assertTrue("Expected unified score >= 85, got ${scores.unifiedLongevityScore}", scores.unifiedLongevityScore >= 85)
    }

    @Test
    fun `poor sleep and suppressed HRV produces Restore readiness`() {
        val biometrics = DailyBiometrics(
            date = LocalDate.of(2026, 11, 21),
            sleep = SleepSummary(
                totalMinutes = 290,
                deepMinutes = 30,
                remMinutes = 45,
                lightMinutes = 215,
                awakeMinutes = 70
            ),
            hrvRmssdMs = 36.0,
            restingHeartRateBpm = 67,
            steps = 3200,
            activeCalories = 180,
            totalCalories = 1750,
            elevatedHrMinutes = 8
        )

        val scores = HealthScoreEngine.scoreDay(biometrics, baseline)

        assertTrue("Expected sleep score < 60, got ${scores.sleepScore}", scores.sleepScore < 60)
        assertTrue("Expected recovery score < 50, got ${scores.recoveryScore}", scores.recoveryScore < 50)
        assertEquals(ReadinessZone.RESTORE, scores.readinessZone)
    }

    @Test
    fun `missing HRV and RHR falls back gracefully without crashing`() {
        val biometrics = DailyBiometrics(
            date = LocalDate.of(2026, 11, 21),
            sleep = SleepSummary(420, 80, 90, 250, 30),
            hrvRmssdMs = null,
            restingHeartRateBpm = null,
            steps = 7000,
            activeCalories = 400,
            totalCalories = 2100,
            elevatedHrMinutes = 25
        )

        val scores = HealthScoreEngine.scoreDay(biometrics, baseline)

        assertEquals(null, scores.hrvDeltaPercent)
        assertEquals(null, scores.rhrDeltaBpm)
        assertTrue(scores.recoveryScore in 1..100)
        assertTrue(scores.unifiedLongevityScore in 1..100)
    }
}
