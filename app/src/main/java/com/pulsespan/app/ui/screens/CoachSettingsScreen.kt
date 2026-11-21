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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pulsespan.app.ui.PulseSpanUiState
import com.pulsespan.app.ui.components.PulseCard
import com.pulsespan.app.ui.theme.PulseColors

@Composable
fun CoachSettingsScreen(
    uiState: PulseSpanUiState,
    onSendQuestion: (String) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val quickPrompts = listOf(
        "What workout fits my readiness today?",
        "How was my sleep architecture?",
        "How can I improve my HRV?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseColors.Background)
            .padding(16.dp)
    ) {
        Text(
            text = "Gemini Longevity Coach",
            style = MaterialTheme.typography.headlineSmall,
            color = PulseColors.TextPrimary
        )
        Text(
            text = "Ask questions grounded in your Fitbit sleep, HRV & strain",
            style = MaterialTheme.typography.bodyMedium,
            color = PulseColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        PulseCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Demo Fitbit Data Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = PulseColors.TextPrimary
                        )
                        Text(
                            text = if (uiState.isDemoMode) "Showing 14-day sample Fitbit data" else "Reading live Health Connect data",
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.preferences.demoModeEnabled,
                        onCheckedChange = onToggleDemoMode
                    )
                }

                if (!uiState.hasHealthPermissions) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseColors.RecoveryGreen)
                    ) {
                        Text("Grant Health Connect Permissions")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = { onSendQuestion(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = PulseColors.SurfaceVariant,
                        labelColor = PulseColors.TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.coachMessages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (msg.isFromUser) PulseColors.AiViolet.copy(alpha = 0.25f)
                                else PulseColors.Surface
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseColors.TextPrimary
                        )
                    }
                }
            }

            if (uiState.isCoachReplying) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PulseColors.AiViolet,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about sleep, HRV, or workouts...", color = PulseColors.TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseColors.AiViolet,
                    unfocusedBorderColor = PulseColors.Border,
                    focusedTextColor = PulseColors.TextPrimary,
                    unfocusedTextColor = PulseColors.TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onSendQuestion(input)
                        input = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = PulseColors.AiViolet
                )
            }
        }
    }
}
