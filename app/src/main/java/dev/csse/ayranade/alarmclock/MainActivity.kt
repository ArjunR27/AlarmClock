package dev.csse.ayranade.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.csse.ayranade.alarmclock.ui.clock.ClockScreen
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmScreen
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmViewModel
import dev.csse.ayranade.alarmclock.ui.alarms.AlarmViewModelFactory
import dev.csse.ayranade.alarmclock.ui.audios.AudioScreen
import dev.csse.ayranade.alarmclock.ui.theme.AppTheme




class MainActivity : ComponentActivity() {
    private val alarmViewModel: AlarmViewModel by viewModels { AlarmViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(dynamicColor = false) {
                AlarmClockApp(alarmViewModel)
            }
        }
    }
}

@Composable
private fun AlarmClockApp(alarmViewModel: AlarmViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "clock") {
        composable("clock") { ClockScreen(navController, alarmViewModel) }
        composable("alarms") { AlarmScreen(navController, alarmViewModel) }
        composable("sounds") { AudioScreen(navController) }
    }
}
