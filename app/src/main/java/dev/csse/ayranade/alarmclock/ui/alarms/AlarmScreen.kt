package dev.csse.ayranade.alarmclock.ui.alarms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

private fun hasNotificationAccess(context: Context): Boolean {
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    if (!notificationsEnabled) {
        return false
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
            Settings.EXTRA_APP_PACKAGE,
            context.packageName
        )
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorDialog(
    availableSounds: List<AlarmSound>,
    initialAlarm: Alarm? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        hour: Int,
        minute: Int,
        label: String,
        soundId: String,
        days: List<Int>,
        am: Boolean,
        snoozeMinutes: Int
    ) -> Unit
) {
    val dialogKey = initialAlarm?.alarmId
    var hour by remember(dialogKey) { mutableStateOf((initialAlarm?.hour ?: 7).toString()) }
    var minute by remember(dialogKey) {
        mutableStateOf(String.format("%02d", initialAlarm?.minute ?: 0))
    }
    var label by remember(dialogKey) { mutableStateOf(initialAlarm?.label.orEmpty()) }
    var selectedDays by remember(dialogKey) {
        mutableStateOf(initialAlarm?.daysOfWeek?.toSet() ?: emptySet())
    }
    var selectedSoundId by remember(dialogKey) {
        mutableStateOf(initialAlarm?.resolvedSoundId ?: DEFAULT_ALARM_STABLE_ID)
    }
    var soundMenuExpanded by remember { mutableStateOf(false) }
    var snoozeMenuExpanded by remember { mutableStateOf(false) }
    var am by remember(dialogKey) { mutableStateOf(initialAlarm?.am ?: true) }
    var selectedSnoozeMinutes by remember(dialogKey) {
        mutableStateOf(
            normalizeSnoozeMinutes(initialAlarm?.snoozeMinutes ?: DEFAULT_SNOOZE_MINUTES)
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dayLabels = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    val selectedSound = availableSounds.firstOrNull { it.stableId == selectedSoundId }
        ?: availableSounds.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAlarm == null) "New Alarm" else "Edit Alarm") },
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formatSnoozeMinutes(selectedSnoozeMinutes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Snooze") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { snoozeMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = snoozeMenuExpanded,
                        onDismissRequest = { snoozeMenuExpanded = false }
                    ) {
                        SNOOZE_MINUTE_OPTIONS.forEach { snoozeMinutes ->
                            DropdownMenuItem(
                                text = { Text(formatSnoozeMinutes(snoozeMinutes)) },
                                onClick = {
                                    selectedSnoozeMinutes = snoozeMinutes
                                    snoozeMenuExpanded = false
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
                    am,
                    selectedSnoozeMinutes
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

@Composable
private fun ExactAlarmPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exact alarm access needed") },
        text = {
            Text("This alarm was saved disabled. To ring at the exact time, allow exact alarms in system settings and then enable it again.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings")
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
private fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification access needed") },
        text = {
            Text("Allow notifications so alarms can show heads-up Dismiss and Snooze controls without opening the app.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings")
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
private fun NotificationPermissionCard(
    onRequestAccess: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable notifications so active alarms can show heads-up Snooze and Dismiss controls.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onRequestAccess) {
                Text("Enable")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    navController: NavController,
    alarmViewModel: AlarmViewModel
) {
    val app = LocalContext.current.applicationContext as AlarmClockApplication
    val context = LocalContext.current
    val audioViewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(app.audioRepository))
    val uiState by alarmViewModel.alarmUiState.collectAsStateWithLifecycle()
    val soundUiState by audioViewModel.soundUiState.collectAsStateWithLifecycle()
    val alarmList = uiState.alarms.values.sortedBy { it.alarmId }
    val availableSounds = soundUiState.defaultSounds + soundUiState.customSounds
    val soundsById = availableSounds.associateBy { it.stableId }
    val notificationsEnabled = hasNotificationAccess(context)
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingAlarmId by remember { mutableStateOf<Int?>(null) }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var selectedAlarmIds by remember { mutableStateOf(setOf<Int>()) }
    val editingAlarm = editingAlarmId?.let(uiState.alarms::get)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showNotificationPermissionDialog = true
        }
    }

    val requestNotificationAccess = {
        when {
            notificationsEnabled -> Unit
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                showNotificationPermissionDialog = true
            }
        }
    }

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
            if (!notificationsEnabled) {
                NotificationPermissionCard(
                    onRequestAccess = requestNotificationAccess
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedAlarmIds.isEmpty()) "Select Alarms" else "${selectedAlarmIds.size} selected",
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
                                Text(
                                    text = "Snooze: ${formatSnoozeMinutes(alarm.snoozeMinutes)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { editingAlarmId = alarm.alarmId }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit alarm")
                            }
                            TextButton(
                                onClick = {
                                    if (alarm.isEnabled) {
                                        alarmViewModel.setAlarmEnabled(alarm.alarmId, false)
                                    } else if (AlarmScheduler.canScheduleExactAlarms(context)) {
                                        alarmViewModel.setAlarmEnabled(alarm.alarmId, true)
                                        requestNotificationAccess()
                                    } else {
                                        showExactAlarmPermissionDialog = true
                                    }
                                }
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
        AlarmEditorDialog(
            availableSounds = availableSounds,
            onDismiss = { showAddDialog = false },
            onConfirm = { hour, minute, label, soundId, days, am, snoozeMinutes ->
                val canSchedule = AlarmScheduler.canScheduleExactAlarms(context)
                alarmViewModel.addAlarm(
                    hour = hour,
                    minute = minute,
                    label = label,
                    soundId = soundId,
                    daysOfWeek = days,
                    am = am,
                    snoozeMinutes = snoozeMinutes,
                    isEnabled = canSchedule
                )
                showAddDialog = false
                if (!canSchedule && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    showExactAlarmPermissionDialog = true
                } else {
                    requestNotificationAccess()
                }
            }
        )
    }

    if (editingAlarm != null) {
        AlarmEditorDialog(
            availableSounds = availableSounds,
            initialAlarm = editingAlarm,
            onDismiss = { editingAlarmId = null },
            onConfirm = { hour, minute, label, soundId, days, am, snoozeMinutes ->
                val canSchedule = AlarmScheduler.canScheduleExactAlarms(context)
                alarmViewModel.updateAlarm(
                    alarmId = editingAlarm.alarmId,
                    hour = hour,
                    minute = minute,
                    label = label,
                    soundId = soundId,
                    daysOfWeek = days,
                    am = am,
                    snoozeMinutes = snoozeMinutes
                )
                editingAlarmId = null
                if (editingAlarm.isEnabled) {
                    if (!canSchedule && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        showExactAlarmPermissionDialog = true
                    } else {
                        requestNotificationAccess()
                    }
                }
            }
        )
    }

    if (showExactAlarmPermissionDialog) {
        ExactAlarmPermissionDialog(
            onDismiss = { showExactAlarmPermissionDialog = false },
            onOpenSettings = {
                showExactAlarmPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(AlarmScheduler.exactAlarmSettingsIntent(context))
                }
            }
        )
    }

    if (showNotificationPermissionDialog) {
        NotificationPermissionDialog(
            onDismiss = { showNotificationPermissionDialog = false },
            onOpenSettings = {
                showNotificationPermissionDialog = false
                context.startActivity(notificationSettingsIntent(context))
            }
        )
    }
}
