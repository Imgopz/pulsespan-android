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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsespan.app.ui.PulseSpanUiState
import com.pulsespan.app.ui.components.PillarScoreTile
import com.pulsespan.app.ui.components.PulseCard
import com.pulsespan.app.ui.components.ScoreRingGauge
import com.pulsespan.app.ui.components.SevenDayBarChart
import com.pulsespan.app.ui.theme.PulseColors
import java.time.format.DateTimeFormatter

@Composable
fun StrainScreen(uiState: PulseSpanUiState) {
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
                text = "Strain & Activity",
                style = MaterialTheme.typography.headlineSmall,
                color = PulseColors.TextPrimary
            )
            Text(
                text = "Cardiovascular load, active burn, and Fitbit exercise sessions",
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
                        score = today.scores.strainScore,
                        label = "Daily Strain",
                        subtitle = "${today.biometrics.elevatedHrMinutes} min elevated HR",
                        accentColor = PulseColors.StrainAmber,
                        size = 168.dp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillarScoreTile(
                    title = "Steps",
                    score = today.biometrics.steps,
                    detail = "Daily movement",
                    accentColor = PulseColors.StrainAmber,
                    modifier = Modifier.weight(1f)
                )
                PillarScoreTile(
                    title = "Active kcal",
                    score = today.biometrics.activeCalories,
                    detail = "${today.biometrics.totalCalories} total kcal",
                    accentColor = PulseColors.RestoreCoral,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            PulseCard {
                Column {
                    Text(
                        text = "7-Day Strain Load",
                        style = MaterialTheme.typography.titleMedium,
                        color = PulseColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SevenDayBarChart(
                        labels = last7.map { it.biometrics.date.format(dayFmt).take(2) },
                        values = last7.map { it.scores.strainScore.toFloat() },
                        accentColor = PulseColors.StrainAmber
                    )
                }
            }
        }

        item {
            Text(
                text = "Today's Fitbit Workouts",
                style = MaterialTheme.typography.titleMedium,
                color = PulseColors.TextPrimary
            )
        }

        if (today.biometrics.workouts.isEmpty()) {
            item {
                PulseCard {
                    Text(
                        text = "No workouts recorded yet today. Sync your Fitbit app to Health Connect after your next session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PulseColors.TextSecondary
                    )
                }
            }
        } else {
            items(today.biometrics.workouts, key = { it.id }) { workout ->
                PulseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = workout.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = PulseColors.TextPrimary
                            )
                            Text(
                                text = "${workout.durationMinutes} min • ${workout.activeCalories} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseColors.TextSecondary
                            )
                        }
                        workout.avgHeartRateBpm?.let { hr ->
                            Text(
                                text = "$hr bpm",
                                style = MaterialTheme.typography.titleMedium,
                                color = PulseColors.StrainAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
