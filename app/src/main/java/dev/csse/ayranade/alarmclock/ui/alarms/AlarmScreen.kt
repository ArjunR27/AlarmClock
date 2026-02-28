package dev.csse.ayranade.alarmclock.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Preview
@Composable
fun AlarmScreenPreview() {
    AlarmScreen(navController = rememberNavController())
}

private fun formatAlarmTime(alarm: Alarm): String {
    return String.format("%02d:%02d", alarm.hour, alarm.minute)
}

private fun formatAlarmDays(daysOfWeek: List<Int>): String {
    if (daysOfWeek.isEmpty()) {
        return "One-time alarm"
    }

    return daysOfWeek.sorted().joinToString(" • ") { day ->
        when (day) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "Day $day"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel = viewModel()
) {
    val uiState by viewModel.alarmUiState.collectAsStateWithLifecycle()

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.addAlarm(
                        hour = 7,
                        minute = 0,
                        label = "Alarm ${uiState.alarms.size + 1}",
                        alarmSoundId = 1,
                        daysOfWeek = emptyList()
                    )
                }
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add alarm")
            }
        }
    ) { innerPadding ->
        val alarmList = uiState.alarms.values.sortedBy { it.alarmId }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(alarmList, key = { it.alarmId }) { alarm ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatAlarmTime(alarm),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (alarm.label.isNotBlank()) {
                                Text(text = alarm.label)
                            }
                            Text(
                                text = formatAlarmDays(alarm.daysOfWeek),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            onClick = { viewModel.setAlarmEnabled(alarm.alarmId, !alarm.isEnabled) }
                        ) {
                            Text(if (alarm.isEnabled) "Enabled" else "Disabled")
                        }
                    }
                }
            }
        }
    }
}
