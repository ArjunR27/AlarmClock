package dev.csse.ayranade.alarmclock.ui.audios


import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.csse.ayranade.alarmclock.AlarmClockApplication
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

// Not implemented
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
                    Text(if (fileUri.isBlank()) "Choose file..." else "File chosen!")
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
    var showDialog by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Default sounds",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(uiState.defaultSounds) { sound ->
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
                        text = "Custom sounds",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
                items(uiState.customSounds) { sound ->
                    SoundCard(
                        sound = sound,
                        context = context,
                        currentPlayer = currentPlayer,
                        onPlayerChange = { currentPlayer = it }
                    )
                }
            }
        }

        if (showDialog) {
            AudioAddDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, fileUri, isCustom ->
                    viewModel.addCustomSound(name = name, uri = fileUri)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun SoundCard(
    sound: AlarmSound,
    context: Context,
    currentPlayer: MediaPlayer?,
    onPlayerChange: (MediaPlayer?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sound.name,
                modifier = Modifier.weight(1f)
            )
            TextButton(
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
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
