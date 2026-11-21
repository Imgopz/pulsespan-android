package com.pulsespan.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulsespan.app.domain.model.SleepSummary
import com.pulsespan.app.ui.theme.PulseColors

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PulseColors.Surface),
        border = BorderStroke(1.dp, PulseColors.Border)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ScoreRingGauge(
    score: Int,
    label: String,
    subtitle: String,
    accentColor: Color,
    size: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val sweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 300f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scoreRingSweep"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)

            drawArc(
                color = PulseColors.SurfaceVariant,
                startAngle = 120f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = 120f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = PulseColors.TextSecondary
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = PulseColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PillarScoreTile(
    title: String,
    score: Int,
    detail: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PulseColors.Surface),
        border = BorderStroke(1.dp, PulseColors.Border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                color = PulseColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
    }
}

@Composable
fun SleepStageBar(sleep: SleepSummary, modifier: Modifier = Modifier) {
    val total = sleep.totalInBedMinutes.coerceAtLeast(1).toFloat()
    val deepWeight = (sleep.deepMinutes / total).coerceAtLeast(0.01f)
    val remWeight = (sleep.remMinutes / total).coerceAtLeast(0.01f)
    val lightWeight = (sleep.lightMinutes / total).coerceAtLeast(0.01f)
    val awakeWeight = (sleep.awakeMinutes / total).coerceAtLeast(0.01f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.weight(deepWeight).height(14.dp).background(PulseColors.DeepSleep))
            Box(modifier = Modifier.weight(remWeight).height(14.dp).background(PulseColors.RemSleep))
            Box(modifier = Modifier.weight(lightWeight).height(14.dp).background(PulseColors.LightSleep))
            Box(modifier = Modifier.weight(awakeWeight).height(14.dp).background(PulseColors.AwakeSleep))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StageLegend("Deep", "${sleep.deepMinutes}m", PulseColors.DeepSleep)
            StageLegend("REM", "${sleep.remMinutes}m", PulseColors.RemSleep)
            StageLegend("Light", "${sleep.lightMinutes}m", PulseColors.LightSleep)
            StageLegend("Awake", "${sleep.awakeMinutes}m", PulseColors.AwakeSleep)
        }
    }
}

@Composable
private fun StageLegend(label: String, value: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = PulseColors.TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = PulseColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SevenDayBarChart(
    labels: List<String>,
    values: List<Float>,
    accentColor: Color,
    unitSuffix: String = "",
    modifier: Modifier = Modifier
) {
    val maxVal = (values.maxOrNull() ?: 100f).coerceAtLeast(1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value / maxVal).coerceIn(0.08f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${value.toInt()}$unitSuffix",
                    style = MaterialTheme.typography.labelMedium,
                    color = PulseColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((72 * fraction).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (index == values.lastIndex) accentColor else accentColor.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == values.lastIndex) PulseColors.TextPrimary else PulseColors.TextSecondary
                )
            }
        }
    }
}
