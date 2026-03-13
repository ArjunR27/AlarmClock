package dev.csse.ayranade.alarmclock.ui.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.csse.ayranade.alarmclock.AlarmClockApplication
import dev.csse.ayranade.alarmclock.ui.audios.AlarmSound
import dev.csse.ayranade.alarmclock.ui.audios.AudioViewModel
import dev.csse.ayranade.alarmclock.ui.audios.AudioViewModelFactory
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_SOUND_NAME
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID

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

private fun formatAlarmSound(alarm: Alarm, soundsById: Map<String, AlarmSound>): String {
    val soundId = alarm.resolvedSoundId
    return soundsById[soundId]?.name
        ?: if (soundId == DEFAULT_ALARM_STABLE_ID) DEFAULT_ALARM_SOUND_NAME else "Missing sound"
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    availableSounds: List<AlarmSound>,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, label: String, soundId: String, days: List<Int>, am: Boolean) -> Unit
) {
    var hour by remember { mutableStateOf("7") }
    var minute by remember { mutableStateOf("00") }
    var label by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var selectedSoundId by remember { mutableStateOf(DEFAULT_ALARM_STABLE_ID) }
    var soundMenuExpanded by remember { mutableStateOf(false) }
    var am by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dayLabels = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val selectedSound = availableSounds.firstOrNull { it.stableId == selectedSoundId }
        ?: availableSounds.firstOrNull()

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
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSound?.name ?: DEFAULT_ALARM_SOUND_NAME,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sound") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { soundMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = soundMenuExpanded,
                        onDismissRequest = { soundMenuExpanded = false }
                    ) {
                        availableSounds.forEach { sound ->
                            DropdownMenuItem(
                                text = { Text(sound.name) },
                                onClick = {
                                    selectedSoundId = sound.stableId
                                    soundMenuExpanded = false
                                }
                            )
                        }
                    }
                }
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
                    selectedSound?.stableId ?: DEFAULT_ALARM_STABLE_ID,
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

@Composable
private fun DeleteAlarmConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete alarms") },
        text = { Text("are you sure you want to delete these alarms?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
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
    alarmViewModel: AlarmViewModel
) {
    val app = LocalContext.current.applicationContext as AlarmClockApplication
    val audioViewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(app.audioRepository))
    val uiState by alarmViewModel.alarmUiState.collectAsStateWithLifecycle()
    val soundUiState by audioViewModel.soundUiState.collectAsStateWithLifecycle()
    val alarmList = uiState.alarms.values.sortedBy { it.alarmId }
    val availableSounds = soundUiState.defaultSounds + soundUiState.customSounds
    val soundsById = availableSounds.associateBy { it.stableId }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedAlarmIds by remember { mutableStateOf(setOf<Int>()) }

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
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add alarm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedAlarmIds.isEmpty()) "Select alarms" else "${selectedAlarmIds.size} selected",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = selectedAlarmIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected alarms")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
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
                            Checkbox(
                                checked = alarm.alarmId in selectedAlarmIds,
                                onCheckedChange = { isChecked ->
                                    selectedAlarmIds = if (isChecked) {
                                        selectedAlarmIds + alarm.alarmId
                                    } else {
                                        selectedAlarmIds - alarm.alarmId
                                    }
                                }
                            )
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
                                Text(
                                    text = "Sound: ${formatAlarmSound(alarm, soundsById)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = { alarmViewModel.setAlarmEnabled(alarm.alarmId, !alarm.isEnabled) }
                            ) {
                                Text(if (alarm.isEnabled) "Enabled" else "Disabled")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteAlarmConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                alarmViewModel.deleteAlarms(selectedAlarmIds)
                selectedAlarmIds = emptySet()
                showDeleteDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddAlarmDialog(
            availableSounds = availableSounds,
            onDismiss = { showAddDialog = false },
            onConfirm = { hour, minute, label, soundId, days, am ->
                alarmViewModel.addAlarm(hour, minute, label, soundId, days, am)
                showAddDialog = false
            }
        )
    }
}
