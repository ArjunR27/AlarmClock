package dev.csse.ayranade.alarmclock.ui.audios

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.csse.ayranade.alarmclock.AlarmClockApplication
import kotlin.random.Random

@Preview
@Composable
fun AudioScreenPreview() {
    AudioScreen(navController = rememberNavController())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, fileUri: String, isCustom: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            fileUri = uri.toString()
            if (name.isBlank()) {
                name = uri.lastPathSegment ?: ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sound") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
                TextButton(onClick = { filePicker.launch(arrayOf("audio/*")) }) {
                    Text(if (fileUri.isBlank()) "Choose file..." else "File chosen")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, fileUri, true) },
                enabled = name.isNotBlank() && fileUri.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteSoundConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete sounds") },
        text = { Text("Are you sure you want to delete these sounds?") },
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
fun AudioScreen(
    navController: NavController,
) {
    val app = LocalContext.current.applicationContext as AlarmClockApplication
    val viewModel: AudioViewModel = viewModel(factory = AudioViewModelFactory(app.audioRepository))
    val uiState by viewModel.soundUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCustomSoundIds by remember { mutableStateOf(setOf<Int>()) }

    DisposableEffect(Unit) {
        onDispose {
            currentPlayer?.release()
            currentPlayer = null
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
                    text = if (selectedCustomSoundIds.isEmpty()) {
                        "Select Custom Sounds"
                    } else {
                        "${selectedCustomSoundIds.size} selected"
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = selectedCustomSoundIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete selected sounds")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Default Sounds",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(uiState.defaultSounds, key = { it.stableId }) { sound ->
                    SoundCard(
                        sound = sound,
                        context = context,
                        currentPlayer = currentPlayer,
                        onPlayerChange = { currentPlayer = it }
                    )
                }

                if (uiState.customSounds.isNotEmpty()) {
                    item {
                        Text(
                            text = "Custom Sounds",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(uiState.customSounds, key = { it.stableId }) { sound ->
                        SoundCard(
                            sound = sound,
                            context = context,
                            currentPlayer = currentPlayer,
                            onPlayerChange = { currentPlayer = it },
                            showSelector = true,
                            isSelected = sound.alarmSoundId in selectedCustomSoundIds,
                            onSelectionChange = { isChecked ->
                                selectedCustomSoundIds = if (isChecked) {
                                    selectedCustomSoundIds + sound.alarmSoundId
                                } else {
                                    selectedCustomSoundIds - sound.alarmSoundId
                                }
                            }
                        )
                    }
                }

                item {
                    AddSoundInlineButton(
                        onClick = { showAddDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteSoundConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                currentPlayer?.release()
                currentPlayer = null
                viewModel.deleteCustomSounds(selectedCustomSoundIds)
                selectedCustomSoundIds = emptySet()
                showDeleteDialog = false
            }
        )
    }

    if (showAddDialog) {
        AudioAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, fileUri, _ ->
                viewModel.addCustomSound(name = name, uri = fileUri)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SoundCard(
    sound: AlarmSound,
    context: Context,
    currentPlayer: MediaPlayer?,
    onPlayerChange: (MediaPlayer?) -> Unit,
    showSelector: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sound.isCustom) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSelector) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked -> onSelectionChange(checked) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            SoundThumbnailTile(isCustom = sound.isCustom)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = sound.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (sound.isCustom) {
                    WaveformPreview(seed = sound.stableId.hashCode())
                } else {
                    Text(
                        text = "Built-in sound",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PlayOutlineButton(
                onClick = {
                    currentPlayer?.release()
                    onPlayerChange(null)

                    val player = MediaPlayer.create(context, sound.fileUri.toUri())
                    if (player != null) {
                        onPlayerChange(player)
                        player.setOnCompletionListener { completedPlayer ->
                            completedPlayer.release()
                            onPlayerChange(null)
                        }
                        player.start()
                    }
                }
            )
        }
    }
}

@Composable
private fun SoundThumbnailTile(isCustom: Boolean) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCustom) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun WaveformPreview(seed: Int) {
    val bars = remember(seed) {
        val random = Random(seed)
        List(20) { random.nextInt(from = 25, until = 100) / 100f }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        bars.forEach { value ->
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = (8 + 20 * value).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
            )
        }
    }
}

@Composable
private fun PlayOutlineButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}

@Composable
private fun AddSoundInlineButton(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .drawWithContent {
                drawContent()
                val strokeWidth = 2.dp.toPx()
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(18f, 12f),
                            phase = 0f
                        )
                    )
                )
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text("Add sound")
        }
    }
}
