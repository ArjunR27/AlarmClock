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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.csse.ayranade.alarmclock.AlarmClockApplication
import dev.csse.ayranade.alarmclock.ui.audios.AlarmSound
import dev.csse.ayranade.alarmclock.ui.audios.AudioViewModel
import dev.csse.ayranade.alarmclock.ui.audios.AudioViewModelFactory
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_SOUND_NAME
import dev.csse.ayranade.alarmclock.ui.audios.DEFAULT_ALARM_STABLE_ID
import kotlinx.coroutines.delay

private val alarmDayLabels = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun",
)

private val compactAlarmDayLabels = listOf(
    1 to "M",
    2 to "T",
    3 to "W",
    4 to "T",
    5 to "F",
    6 to "S",
    7 to "S",
)

private fun formatAlarmTime(alarm: Alarm): String =
    String.format("%02d:%02d", normalizedDisplayHour(alarm.hour), alarm.minute.coerceIn(0, 59))

private fun formatAlarmSound(alarm: Alarm, soundsById: Map<String, AlarmSound>): String {
    val soundId = alarm.resolvedSoundId
    return soundsById[soundId]?.name
        ?: if (soundId == DEFAULT_ALARM_STABLE_ID) DEFAULT_ALARM_SOUND_NAME else "Missing sound"
}

private fun parseSnoozeMinutes(input: String): Int? =
    input.toIntOrNull()?.takeIf { it in MIN_SNOOZE_MINUTES..MAX_SNOOZE_MINUTES }

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
private fun AlarmEditorSheet(
    availableSounds: List<AlarmSound>,
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
    var hour by rememberSaveable { mutableStateOf("7") }
    var minute by rememberSaveable { mutableStateOf("00") }
    var label by rememberSaveable { mutableStateOf("") }
    var selectedDays by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var selectedSoundId by rememberSaveable { mutableStateOf(DEFAULT_ALARM_STABLE_ID) }
    var soundMenuExpanded by remember { mutableStateOf(false) }
    var am by rememberSaveable { mutableStateOf(true) }
    var snoozeMinutesText by rememberSaveable { mutableStateOf(DEFAULT_SNOOZE_MINUTES.toString()) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedSound = availableSounds.firstOrNull { it.stableId == selectedSoundId }
        ?: availableSounds.firstOrNull()
    val parsedSnoozeMinutes = parseSnoozeMinutes(snoozeMinutesText)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val maxSheetHeight = if (maxWidth > maxHeight) maxHeight * 0.86f else maxHeight * 0.94f

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .heightIn(max = maxSheetHeight)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Alarm",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    hour.toIntOrNull() ?: 0,
                                    minute.toIntOrNull() ?: 0,
                                    label,
                                    selectedSound?.stableId ?: DEFAULT_ALARM_STABLE_ID,
                                    selectedDays,
                                    am,
                                    parsedSnoozeMinutes ?: DEFAULT_SNOOZE_MINUTES
                                )
                            },
                            enabled = parsedSnoozeMinutes != null
                        ) {
                            Text("Save")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hour,
                            onValueChange = { if (it.length <= 2) hour = it },
                            label = { Text("Hour") },
                            singleLine = true,
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
                            singleLine = true,
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
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Label") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
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
                    OutlinedTextField(
                        value = snoozeMinutesText,
                        onValueChange = {
                            snoozeMinutesText = it.filter(Char::isDigit).take(3)
                        },
                        label = { Text("Snooze (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = parsedSnoozeMinutes == null,
                        supportingText = {
                            Text("Enter $MIN_SNOOZE_MINUTES-$MAX_SNOOZE_MINUTES minutes")
                        }
                    )
                    Text("Repeat", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        alarmDayLabels.forEach { (dayNum, dayName) ->
                            DayToggleChip(
                                label = dayName.take(1),
                                isSelected = dayNum in selectedDays,
                                onClick = {
                                    selectedDays = if (dayNum in selectedDays) {
                                        selectedDays - dayNum
                                    } else {
                                        (selectedDays + dayNum).sorted()
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeleteAlarmConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete alarms") },
        text = { Text("Are you sure you want to delete these alarms?") },
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

@Composable
private fun DayToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun AlarmSoundPill(
    soundName: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = soundName,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    soundName: String,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onLabelCommit: (String) -> Unit,
    onTimeClick: () -> Unit,
    onDayToggle: (Int) -> Unit,
    onSoundClick: () -> Unit,
    onSnoozeCommit: (Int) -> Unit
) {
    var labelText by remember(alarm.alarmId, alarm.label) { mutableStateOf(alarm.label) }
    var snoozeText by remember(alarm.alarmId, alarm.snoozeMinutes) {
        mutableStateOf(alarm.snoozeMinutes.toString())
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val parsedSnoozeMinutes = parseSnoozeMinutes(snoozeText)

    fun commitLabel() {
        if (labelText != alarm.label) {
            onLabelCommit(labelText)
        }
    }

    fun commitSnooze() {
        val snoozeMinutes = parsedSnoozeMinutes
        if (snoozeMinutes == null) {
            snoozeText = alarm.snoozeMinutes.toString()
            return
        }

        if (snoozeMinutes != alarm.snoozeMinutes) {
            onSnoozeCommit(snoozeMinutes)
        }
    }

    LaunchedEffect(alarm.label) {
        if (labelText != alarm.label) {
            labelText = alarm.label
        }
    }

    LaunchedEffect(alarm.snoozeMinutes) {
        val normalizedAlarmSnooze = alarm.snoozeMinutes.toString()
        if (snoozeText != normalizedAlarmSnooze) {
            snoozeText = normalizedAlarmSnooze
        }
    }

    LaunchedEffect(labelText, alarm.label) {
        if (labelText != alarm.label) {
            delay(350)
            if (labelText != alarm.label) {
                onLabelCommit(labelText)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onTimeClick)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatAlarmTime(alarm),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Surface(
                            modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = if (alarm.am) "AM" else "PM",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            TextField(
                value = labelText,
                onValueChange = { labelText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            commitLabel()
                        }
                    },
                placeholder = { Text("Alarm label") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commitLabel()
                        keyboardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            AlarmSoundPill(
                soundName = soundName,
                onClick = onSoundClick
            )

            OutlinedTextField(
                value = snoozeText,
                onValueChange = {
                    snoozeText = it.filter(Char::isDigit).take(3)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            commitSnooze()
                        }
                    },
                label = { Text("Snooze (minutes)") },
                singleLine = true,
                isError = parsedSnoozeMinutes == null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commitSnooze()
                        keyboardController?.hide()
                    }
                ),
                supportingText = {
                    Text("Enter $MIN_SNOOZE_MINUTES-$MAX_SNOOZE_MINUTES minutes")
                }
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                compactAlarmDayLabels.forEach { (dayNumber, label) ->
                    DayToggleChip(
                        label = label,
                        isSelected = dayNumber in alarm.daysOfWeek,
                        onClick = { onDayToggle(dayNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmTimePickerDialog(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, am: Boolean) -> Unit
) {
    var hourText by remember(alarm.alarmId) {
        mutableStateOf(normalizedDisplayHour(alarm.hour).toString())
    }
    var minuteText by remember(alarm.alarmId) {
        mutableStateOf(String.format("%02d", alarm.minute.coerceIn(0, 59)))
    }
    var isAm by remember(alarm.alarmId) { mutableStateOf(alarm.am) }
    val parsedHour = hourText.toIntOrNull()?.takeIf { it in 1..12 }
    val parsedMinute = minuteText.toIntOrNull()?.takeIf { it in 0..59 }
    val isValidTime = parsedHour != null && parsedMinute != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = hourText,
                    onValueChange = {
                        val digitsOnly = it.filter(Char::isDigit).take(2)
                        hourText = digitsOnly
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    placeholder = {
                        Text(
                            text = "07",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp)
                        )
                    }
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                TextField(
                    value = minuteText,
                    onValueChange = {
                        val digitsOnly = it.filter(Char::isDigit).take(2)
                        minuteText = digitsOnly
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    placeholder = {
                        Text(
                            text = "00",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 42.sp)
                        )
                    }
                )
                TextButton(onClick = { isAm = !isAm }) {
                    Text(
                        text = if (isAm) "AM" else "PM",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(parsedHour ?: return@TextButton, parsedMinute ?: return@TextButton, isAm)
                },
                enabled = isValidTime
            ) {
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
private fun AlarmSoundPickerSheet(
    availableSounds: List<AlarmSound>,
    selectedSoundId: String,
    onDismiss: () -> Unit,
    onSoundSelected: (AlarmSound) -> Unit
) {
    val customSounds = availableSounds.filter { it.isCustom }
    val defaultSounds = availableSounds.filterNot { it.isCustom }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Choose sound",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (customSounds.isNotEmpty()) {
                Text(
                    text = "Uploaded sounds",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                customSounds.forEach { sound ->
                    SoundSheetRow(
                        sound = sound,
                        isSelected = sound.stableId == selectedSoundId,
                        onClick = { onSoundSelected(sound) }
                    )
                }
            }

            Text(
                text = "Built-in sounds",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            defaultSounds.forEach { sound ->
                SoundSheetRow(
                    sound = sound,
                    isSelected = sound.stableId == selectedSoundId,
                    onClick = { onSoundSelected(sound) }
                )
            }
        }
    }
}

@Composable
private fun SoundSheetRow(
    sound: AlarmSound,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = sound.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (sound.isCustom) "Uploaded file" else "Built-in sound",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    alarmViewModel: AlarmViewModel,
    contentPadding: PaddingValues
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
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showExactAlarmPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var selectedAlarmIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var soundPickerAlarmId by rememberSaveable { mutableStateOf<Int?>(null) }
    var timePickerAlarmId by rememberSaveable { mutableStateOf<Int?>(null) }
    val soundPickerAlarm = soundPickerAlarmId?.let(uiState.alarms::get)
    val timePickerAlarm = timePickerAlarmId?.let(uiState.alarms::get)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    text = if (selectedAlarmIds.isEmpty()) {
                        "Select Alarms"
                    } else {
                        "${selectedAlarmIds.size} selected"
                    },
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
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(alarmList, key = { it.alarmId }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        soundName = formatAlarmSound(alarm, soundsById),
                        isSelected = alarm.alarmId in selectedAlarmIds,
                        onSelectionChange = { isChecked ->
                            selectedAlarmIds = if (isChecked) {
                                (selectedAlarmIds + alarm.alarmId).distinct()
                            } else {
                                selectedAlarmIds - alarm.alarmId
                            }
                        },
                        onToggleEnabled = { isEnabled ->
                            if (!isEnabled) {
                                alarmViewModel.setAlarmEnabled(alarm.alarmId, false)
                            } else if (AlarmScheduler.canScheduleExactAlarms(context)) {
                                alarmViewModel.setAlarmEnabled(alarm.alarmId, true)
                                requestNotificationAccess()
                            } else {
                                showExactAlarmPermissionDialog = true
                            }
                        },
                        onLabelCommit = { label ->
                            alarmViewModel.updateAlarmLabel(alarm.alarmId, label)
                        },
                        onTimeClick = {
                            timePickerAlarmId = alarm.alarmId
                        },
                        onDayToggle = { day ->
                            alarmViewModel.toggleAlarmDay(alarm.alarmId, day)
                        },
                        onSoundClick = {
                            soundPickerAlarmId = alarm.alarmId
                        },
                        onSnoozeCommit = { snoozeMinutes ->
                            alarmViewModel.updateAlarmSnooze(alarm.alarmId, snoozeMinutes)
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = "Add alarm")
        }
    }

    if (showDeleteDialog) {
        DeleteAlarmConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                alarmViewModel.deleteAlarms(selectedAlarmIds.toSet())
                selectedAlarmIds = emptyList()
                showDeleteDialog = false
            }
        )
    }

    if (showAddDialog) {
        AlarmEditorSheet(
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

    if (timePickerAlarm != null) {
        AlarmTimePickerDialog(
            alarm = timePickerAlarm,
            onDismiss = { timePickerAlarmId = null },
            onConfirm = { hour, minute, am ->
                alarmViewModel.updateAlarmTime(
                    alarmId = timePickerAlarm.alarmId,
                    hour = hour,
                    minute = minute,
                    am = am
                )
                timePickerAlarmId = null
            }
        )
    }

    if (soundPickerAlarm != null) {
        AlarmSoundPickerSheet(
            availableSounds = availableSounds,
            selectedSoundId = soundPickerAlarm.resolvedSoundId,
            onDismiss = { soundPickerAlarmId = null },
            onSoundSelected = { sound ->
                alarmViewModel.updateAlarmSound(soundPickerAlarm.alarmId, sound.stableId)
                soundPickerAlarmId = null
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
