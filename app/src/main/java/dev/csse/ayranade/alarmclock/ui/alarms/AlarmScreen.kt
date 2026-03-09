package dev.csse.ayranade.alarmclock.ui.alarms

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.selects.select

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, label: String, soundId: Int, days: List<Int>, am: Boolean) -> Unit
) {
    var hour by remember { mutableStateOf("7") }
    var minute by remember { mutableStateOf("00") }
    var label by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var selectedSoundId by remember { mutableIntStateOf(1) }
    var am by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dayLabels = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2) hour = it },
                        label = { Text("Hour") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2) minute = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    // AM/PM toggle
                    TextButton(
                        onClick = { am = !am },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(if (am) "AM" else "PM")
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    )
                )
                Text("Repeat", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEach { (dayNum, dayName) ->
                        FilterChip(
                            selected = dayNum in selectedDays,
                            onClick = {
                                selectedDays = if (dayNum in selectedDays) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            },
                            label = { Text(dayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    hour.toIntOrNull() ?: 0,
                    minute.toIntOrNull() ?: 0,
                    label,
                    selectedSoundId,
                    selectedDays.toList(),
                    am
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel
) {
    val uiState by viewModel.alarmUiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

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
                onClick = { showDialog = true }
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

    if (showDialog) {
        AddAlarmDialog(
            onDismiss = { showDialog = false },
            onConfirm = { hour, minute, label, soundId, days, am ->
                viewModel.addAlarm(hour, minute, label, soundId, days, am)
                showDialog = false
            }
        )
    }
}