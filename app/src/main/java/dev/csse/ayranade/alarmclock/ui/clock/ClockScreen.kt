package dev.csse.ayranade.alarmclock.ui.clock


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin


fun DrawScope.drawHand(center: Offset, length: Float, angle: Double, color: Color, strokeWidth: Float) {
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

@Composable
@Preview
fun ClockScreen(viewModel: ClockViewModel = viewModel()) {
    val uiState by viewModel.clockUiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier= Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            drawCircle(color = Color.White, radius=radius, center=center)
            drawCircle(color = Color.Black, radius=radius, center=center,
                style = Stroke(width = 6f)
            )

            // Hour markers
            for (i in 0..11) {
                val angle = Math.toRadians((i * 30 - 90).toDouble())
                val start = Offset(
                    x = center.x + (radius * 0.85f * cos(angle)).toFloat(),
                    y = center.y + (radius * 0.85f * sin(angle)).toFloat()
                )

                val end = Offset(
                    x = center.x + (radius * 0.95f * cos(angle)).toFloat(),
                    y = center.y + (radius * 0.95f * sin(angle)).toFloat(),
                )

                drawLine(color = Color.Black, start = start, end = end, strokeWidth = 6f)
            }

            // 360 degrees / 12 hours = 30 degrees per hour
            // 30 degrees per hour, each hour has 60 minutes --> 30/60 = 0.5
            val hourAngle = Math.toRadians(((uiState.hour * 30 + uiState.minute * 0.5) - 90))
            drawHand(center=center, length=radius * 0.5f, angle=hourAngle, color=Color.Black, strokeWidth=12f)

            // 360 degrees / 60 minutes = 6 degrees per minute
            // 6 degrees per minute, each minute has 60 seconds -- 6 / 60  = 0.1
            val minuteAngle = Math.toRadians(((uiState.minute * 6 + uiState.second * 0.1) - 90))
            drawHand(center=center, length=radius * 0.7f, angle=minuteAngle, color=Color.Black, strokeWidth=8f)

            // 360 degrees / 60 seconds = 6 degrees per second
            val secondAngle = Math.toRadians((uiState.second * 6 - 90).toDouble())
            drawHand(center=center, length=radius * 0.8f, angle=secondAngle, color=Color.Red, strokeWidth=4f)

            drawCircle(color = Color.Black, radius = 10f, center = center)
        }
        Column {
            Text(text = "Seconds: ${uiState.second}\n")
            Text(text = "Minute: ${uiState.minute}\n")
            Text(text = "Hour: ${uiState.hour}\n")
        }
    }
}
