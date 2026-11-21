package com.pulsespan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pulsespan.app.ui.components.PulseCard
import com.pulsespan.app.ui.theme.PulseColors
import com.pulsespan.app.ui.theme.PulseSpanTheme

/**
 * Required by Android Health Connect & Google Play policy to explain how PulseSpan
 * uses Fitbit biometric data read from Health Connect.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseSpanTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PulseColors.Background)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    PulseCard {
                        Column {
                            Text(
                                text = stringResource(R.string.health_permissions_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = PulseColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.health_permissions_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { finish() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseColors.RecoveryGreen)
                            ) {
                                Text("Got it")
                            }
                        }
                    }
                }
            }
        }
    }
}
