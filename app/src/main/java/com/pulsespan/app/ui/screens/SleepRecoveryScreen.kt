package com.pulsespan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulsespan.app.ui.PulseSpanUiState
import com.pulsespan.app.ui.components.PillarScoreTile
import com.pulsespan.app.ui.components.PulseCard
import com.pulsespan.app.ui.components.ScoreRingGauge
import com.pulsespan.app.ui.components.SevenDayBarChart
import com.pulsespan.app.ui.components.SleepStageBar
import com.pulsespan.app.ui.theme.PulseColors
import java.time.format.DateTimeFormatter

@Composable
fun SleepRecoveryScreen(uiState: PulseSpanUiState) {
    val today = uiState.today ?: return
    val last7 = uiState.last7Days
    val dayFmt = DateTimeFormatter.ofPattern("E")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sleep & Recovery",
                style = MaterialTheme.typography.headlineSmall,
                color = PulseColors.TextPrimary
            )
            Text(
                text = "Fitbit sleep stages, overnight HRV, and resting heart rate vs. 14-day baseline",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseColors.TextSecondary
            )
        }

        item {
            PulseCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScoreRingGauge(
                        score = today.scores.sleepScore,
                        label = "Sleep Score",
                        subtitle = "${today.biometrics.sleep.formattedDuration} • ${today.biometrics.sleep.efficiencyPercent}% eff.",
                        accentColor = PulseColors.SleepCyan,
                        size = 168.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SleepStageBar(sleep = today.biometrics.sleep)
                }
            }
        }

        item {
            val hrvVal = today.biometrics.hrvRmssdMs?.toInt() ?: 0
            val hrvDelta = today.scores.hrvDeltaPercent?.let { d ->
                if (d >= 0) "+$d% vs baseline" else "$d% vs baseline"
            } ?: "Baseline ${uiState.baseline.avgHrvRmssdMs.toInt()} ms"

            val rhrVal = today.biometrics.restingHeartRateBpm ?: 0
            val rhrDelta = today.scores.rhrDeltaBpm?.let { d ->
                when {
                    d < 0 -> "$d bpm (strong)"
                    d > 0 -> "+$d bpm (elevated)"
                    else -> "At baseline"
                }
            } ?: "Baseline ${uiState.baseline.avgRestingHeartRateBpm.toInt()} bpm"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillarScoreTile(
                    title = "Overnight HRV (ms)",
                    score = hrvVal,
                    detail = hrvDelta,
                    accentColor = PulseColors.RecoveryGreen,
                    modifier = Modifier.weight(1f)
                )
                PillarScoreTile(
                    title = "Resting HR (bpm)",
                    score = rhrVal,
                    detail = rhrDelta,
                    accentColor = PulseColors.SleepCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            PulseCard {
                Column {
                    Text(
                        text = "7-Day Overnight HRV Trend (ms)",
                        style = MaterialTheme.typography.titleMedium,
                        color = PulseColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "14-day personal baseline: ${"%.1f".format(uiState.baseline.avgHrvRmssdMs)} ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SevenDayBarChart(
                        labels = last7.map { it.biometrics.date.format(dayFmt).take(2) },
                        values = last7.map { (it.biometrics.hrvRmssdMs ?: 0.0).toFloat() },
                        accentColor = PulseColors.RecoveryGreen
                    )
                }
            }
        }
    }
}
