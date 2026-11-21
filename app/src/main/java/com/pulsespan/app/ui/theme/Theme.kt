package com.pulsespan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PulseColors {
    val Background = Color(0xFF0B0F14)
    val Surface = Color(0xFF131A22)
    val SurfaceVariant = Color(0xFF1B2430)
    val Border = Color(0xFF263242)

    val TextPrimary = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFF94A3B8)

    val RecoveryGreen = Color(0xFF22C55E)
    val SleepCyan = Color(0xFF38BDF8)
    val StrainAmber = Color(0xFFF59E0B)
    val RestoreCoral = Color(0xFFEF4444)
    val AiViolet = Color(0xFFA855F7)

    val DeepSleep = Color(0xFF6366F1)
    val RemSleep = Color(0xFF38BDF8)
    val LightSleep = Color(0xFF334155)
    val AwakeSleep = Color(0xFFF97316)
}

private val DarkColorScheme = darkColorScheme(
    primary = PulseColors.RecoveryGreen,
    onPrimary = Color(0xFF04140A),
    secondary = PulseColors.SleepCyan,
    onSecondary = Color(0xFF03141F),
    tertiary = PulseColors.AiViolet,
    background = PulseColors.Background,
    onBackground = PulseColors.TextPrimary,
    surface = PulseColors.Surface,
    onSurface = PulseColors.TextPrimary,
    surfaceVariant = PulseColors.SurfaceVariant,
    onSurfaceVariant = PulseColors.TextSecondary,
    outline = PulseColors.Border
)

private val PulseTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun PulseSpanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = PulseTypography,
        content = content
    )
}
