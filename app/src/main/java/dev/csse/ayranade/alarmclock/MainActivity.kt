package dev.csse.ayranade.alarmclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
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

private data class TopLevelDestination(
    val route: String,
    val label: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(route = "clock", label = "Clock"),
    TopLevelDestination(route = "alarms", label = "Alarms"),
    TopLevelDestination(route = "sounds", label = "Sounds")
)

@Composable
private fun AlarmClockApp(alarmViewModel: AlarmViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedTabIndex = topLevelDestinations.indexOfFirst { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }.coerceAtLeast(0)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp
            ) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    topLevelDestinations.forEach { destination ->
                        Tab(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            text = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "clock",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("clock") { ClockScreen(alarmViewModel = alarmViewModel, contentPadding = innerPadding) }
            composable("alarms") { AlarmScreen(alarmViewModel = alarmViewModel, contentPadding = innerPadding) }
            composable("sounds") { AudioScreen(contentPadding = innerPadding) }
        }
    }
}
