package dev.csse.ayranade.alarmclock.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmViewModel
import dev.csse.ayranade.alarmclock.ui.alarms.formatNextAlarmCountdown
import dev.csse.ayranade.alarmclock.ui.alarms.nextEnabledAlarm
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawHand(
    center: Offset,
    length: Float,
    angle: Double,
    color: Color,
    strokeWidth: Float
) {
    val end = Offset(
        x = center.x + (length * cos(angle)).toFloat(),
        y = center.y + (length * sin(angle)).toFloat()
    )

    drawLine(
        color = color,
        start = center,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@Preview
@Composable
fun ClockScreenPreview() {
    ClockScreenContent(
        uiState = ClockUiState(hour = 8, minute = 24, second = 15),
        dateText = "Monday, March 16",
        nextAlarmText = "Next alarm in 1h 12m",
        contentPadding = PaddingValues()
    )
}

@Composable
fun ClockScreen(
    alarmViewModel: AlarmViewModel,
    contentPadding: PaddingValues,
    viewModel: ClockViewModel = viewModel()
) {
    val clockUiState by viewModel.clockUiState.collectAsStateWithLifecycle()
    val alarmUiState by alarmViewModel.alarmUiState.collectAsStateWithLifecycle()
    val now = ZonedDateTime.now()
    val nextAlarmText = nextEnabledAlarm(alarmUiState.alarms.values, now)?.let { upcomingAlarm ->
        formatNextAlarmCountdown(upcomingAlarm.triggerAt, now)
    }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    ClockScreenContent(
        uiState = clockUiState,
        dateText = dateText,
        nextAlarmText = nextAlarmText,
        contentPadding = contentPadding
    )
}

@Composable
private fun ClockScreenContent(
    uiState: ClockUiState,
    dateText: String,
    nextAlarmText: String?,
    contentPadding: PaddingValues
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val clockSize = if (isLandscape) {
            minOf(maxHeight * 0.72f, maxWidth * 0.44f).coerceIn(180.dp, 320.dp)
        } else {
            minOf(maxWidth * 0.82f, maxHeight * 0.45f).coerceIn(220.dp, 360.dp)
        }

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnalogClockFace(
                        uiState = uiState,
                        modifier = Modifier.size(clockSize)
                    )
                }
                ClockSummary(
                    uiState = uiState,
                    dateText = dateText,
                    nextAlarmText = nextAlarmText,
                    modifier = Modifier.weight(1f),
                    isLandscape = true
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnalogClockFace(
                    uiState = uiState,
                    modifier = Modifier.size(clockSize)
                )
                Spacer(modifier = Modifier.size(24.dp))
                ClockSummary(
                    uiState = uiState,
                    dateText = dateText,
                    nextAlarmText = nextAlarmText,
                    modifier = Modifier.fillMaxWidth(),
                    isLandscape = false
                )
            }
        }
    }
}

@Composable
private fun AnalogClockFace(
    uiState: ClockUiState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        drawCircle(color = Color.White, radius = radius, center = center)
        drawCircle(
            color = Color.Black,
            radius = radius,
            center = center,
            style = Stroke(width = 6f)
        )

        for (i in 0..11) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val start = Offset(
                x = center.x + (radius * 0.85f * cos(angle)).toFloat(),
                y = center.y + (radius * 0.85f * sin(angle)).toFloat()
            )

            val end = Offset(
                x = center.x + (radius * 0.95f * cos(angle)).toFloat(),
                y = center.y + (radius * 0.95f * sin(angle)).toFloat()
            )

            drawLine(color = Color.Black, start = start, end = end, strokeWidth = 6f)
        }

        val hourAngle =
            Math.toRadians(((uiState.hour * 30 + uiState.minute * 0.5) - 90))
        drawHand(
            center = center,
            length = radius * 0.5f,
            angle = hourAngle,
            color = Color.Black,
            strokeWidth = 12f
        )

        val minuteAngle =
            Math.toRadians(((uiState.minute * 6 + uiState.second * 0.1) - 90))
        drawHand(
            center = center,
            length = radius * 0.7f,
            angle = minuteAngle,
            color = Color.Black,
            strokeWidth = 8f
        )

        val secondAngle = Math.toRadians((uiState.second * 6 - 90).toDouble())
        drawHand(
            center = center,
            length = radius * 0.8f,
            angle = secondAngle,
            color = Color.Red,
            strokeWidth = 4f
        )

        drawCircle(color = Color.Black, radius = 10f, center = center)
    }
}

@Composable
private fun ClockSummary(
    uiState: ClockUiState,
    dateText: String,
    nextAlarmText: String?,
    modifier: Modifier = Modifier,
    isLandscape: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 8.dp)
    ) {
        Text(
            text = String.format("%02d:%02d", uiState.hour, uiState.minute),
            fontSize = if (isLandscape) 72.sp else 88.sp,
            lineHeight = if (isLandscape) 76.sp else 92.sp
        )
        Text(
            text = dateText,
            style = if (isLandscape) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineMedium
            }
        )
        if (nextAlarmText != null) {
            NextAlarmChip(text = nextAlarmText)
        }
    }
}

@Composable
private fun NextAlarmChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {}
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
