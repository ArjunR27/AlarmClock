package dev.csse.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import dev.csse.alarmclock.ui.theme.AlarmClockTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.csse.ayranade.alarmclock.ui.clock.ClockScreen
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmScreen
import dev.csse.ayranade.alarmclock.ui.audios.AudioScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarmClockTheme {
                AlarmClockApp()
            }
        }
    }
}

@Composable
private fun AlarmClockApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "clock") {
        composable("clock") { ClockScreen(navController) }
        composable("alarms") { AlarmScreen(navController) }
        composable("sounds") { AudioScreen(navController) }
    }
}
