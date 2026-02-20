package dev.csse.ayranade.alarmclock.ui.audios

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
import androidx.compose.material.icons.filled.Add
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

// Not implemented


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun AudioScreen(viewModel: SoundsViewModel = viewModel()) {
    val uiState by viewModel.soundUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

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
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) { Text("Clock") }
                        TextButton(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) { Text("Alarms") }
                        TextButton(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) { Text("Sounds") }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { }
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
            items(uiState.defaultSounds) { sound ->
                Card(
                    modifier = Modifier.fillMaxWidth()) {
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
                                currentPlayer = null

                                val player = MediaPlayer.create(context, sound.fileUri.toUri())
                                if (player != null) {
                                    currentPlayer = player
                                    player.setOnCompletionListener { completedPlayer ->
                                        completedPlayer.release()
                                        if (currentPlayer == completedPlayer) {
                                            currentPlayer = null
                                        }
                                    }
                                    player.start()
                                }
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Add")
                        }
                    }
                }
            }
        }
    }
}
