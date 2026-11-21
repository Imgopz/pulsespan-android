package com.pulsespan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsespan.app.domain.model.ReadinessZone
import com.pulsespan.app.ui.PulseSpanUiState
import com.pulsespan.app.ui.components.PillarScoreTile
import com.pulsespan.app.ui.components.PulseCard
import com.pulsespan.app.ui.components.ScoreRingGauge
import com.pulsespan.app.ui.components.SevenDayBarChart
import com.pulsespan.app.ui.theme.PulseColors
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    uiState: PulseSpanUiState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onOpenCoach: () -> Unit
) {
    val today = uiState.today

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PulseSpan",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PulseColors.TextPrimary
                    )
                    Text(
                        text = if (uiState.isDemoMode) "Demo Fitbit Data • Health Connect Ready" else "Synced with Google Fitbit via Health Connect",
                        style = MaterialTheme.typography.labelMedium,
                        color = PulseColors.TextSecondary
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Fitbit data",
                        tint = PulseColors.TextPrimary
                    )
                }
            }
        }

        if (!uiState.hasHealthPermissions) {
            item {
                PulseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = PulseColors.SleepCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Connect Google Fitbit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PulseColors.TextPrimary
                                )
                                Text(
                                    text = "Grant Health Connect read access to sync live sleep, HRV & strain.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PulseColors.TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = PulseColors.RecoveryGreen)
                        ) {
                            Text("Connect", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (today != null) {
            val zoneColor = when (today.scores.readinessZone) {
                ReadinessZone.PRIME -> PulseColors.RecoveryGreen
                ReadinessZone.BALANCED -> PulseColors.StrainAmber
                ReadinessZone.RESTORE -> PulseColors.RestoreCoral
            }

            item {
                PulseCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScoreRingGauge(
                            score = today.scores.unifiedLongevityScore,
                            label = "Longevity Score",
                            subtitle = "${today.scores.readinessZone.label} Readiness",
                            accentColor = zoneColor,
                            size = 196.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = today.scores.readinessZone.guidance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseColors.TextSecondary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PillarScoreTile(
                        title = "Sleep",
                        score = today.scores.sleepScore,
                        detail = today.biometrics.sleep.formattedDuration,
                        accentColor = PulseColors.SleepCyan,
                        modifier = Modifier.weight(1f)
                    )
                    PillarScoreTile(
                        title = "Recovery",
                        score = today.scores.recoveryScore,
                        detail = today.biometrics.hrvRmssdMs?.let { "${it.toInt()} ms HRV" } ?: "HRV N/A",
                        accentColor = PulseColors.RecoveryGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PillarScoreTile(
                        title = "Strain",
                        score = today.scores.strainScore,
                        detail = "${today.biometrics.activeCalories} kcal",
                        accentColor = PulseColors.StrainAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                PulseCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PulseColors.AiViolet.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PulseColors.AiViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini Daily Briefing",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PulseColors.TextPrimary
                                )
                            }
                            Button(
                                onClick = onOpenCoach,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseColors.SurfaceVariant)
                            ) {
                                Text("Ask Coach", color = PulseColors.AiViolet, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        if (uiState.isBriefingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PulseColors.AiViolet,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = uiState.dailyBriefing,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseColors.TextPrimary
                            )
                        }
                    }
                }
            }

            item {
                val dayFmt = DateTimeFormatter.ofPattern("E")
                val last7 = uiState.last7Days
                PulseCard {
                    Column {
                        Text(
                            text = "7-Day Longevity Trend",
                            style = MaterialTheme.typography.titleMedium,
                            color = PulseColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SevenDayBarChart(
                            labels = last7.map { it.biometrics.date.format(dayFmt).take(2) },
                            values = last7.map { it.scores.unifiedLongevityScore.toFloat() },
                            accentColor = PulseColors.RecoveryGreen
                        )
                    }
                }
            }
        }
    }
}
