package dev.csse.ayranade.alarmclock.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmScreen
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

@Preview
@Composable
fun ClockScreenPreview() {
    ClockScreen(navController = rememberNavController())
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    navController: NavController,
    viewModel: ClockViewModel = viewModel()
) {
    val uiState by viewModel.clockUiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { navController.navigate("clock") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clock") }
                        TextButton(
                            onClick = { navController.navigate("alarms") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Alarms") }
                        TextButton(
                            onClick = { navController.navigate("sounds") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Sounds") }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(modifier = Modifier.size(300.dp).offset(y = (-90).dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                drawCircle(color = Color.White, radius = radius, center = center)
                drawCircle(
                    color = Color.Black, radius = radius, center = center,
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
                drawHand(center = center, length = radius * 0.5f, angle = hourAngle, color = Color.Black, strokeWidth = 12f)

                // 360 degrees / 60 minutes = 6 degrees per minute
                // 6 degrees per minute, each minute has 60 seconds -- 6 / 60  = 0.1
                val minuteAngle = Math.toRadians(((uiState.minute * 6 + uiState.second * 0.1) - 90))
                drawHand(center = center, length = radius * 0.7f, angle = minuteAngle, color = Color.Black, strokeWidth = 8f)

                // 360 degrees / 60 seconds = 6 degrees per second
                val secondAngle = Math.toRadians((uiState.second * 6 - 90).toDouble())
                drawHand(center = center, length = radius * 0.8f, angle = secondAngle, color = Color.Red, strokeWidth = 4f)

                drawCircle(color = Color.Black, radius = 10f, center = center)
            }

            Text (
                text = String.format("%02d:%02d", uiState.hour, uiState.minute),
                fontSize = 64.sp,
                modifier = Modifier.offset(y = (-60).dp)
            )

            val today = remember { java.time.LocalDate.now() }
            val dateText = today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))

            Text (
                text = dateText,
                fontSize = 32.sp,
                modifier = Modifier.offset(y = (-50).dp)
            )

        }
    }
}