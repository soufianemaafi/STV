package com.example.stv

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.stv.ui.theme.STVTheme

@UnstableApi
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        val videoUrl = intent.getStringExtra("VIDEO_URL")

        setContent {
            STVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    if (videoUrl != null) {
                        VideoPlayer(videoUrl)
                    } else {
                        ErrorScreen("URL non fournie")
                    }
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current

    // ExoPlayer state
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // UI state
    var showQualityDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Track selection state
    val videoTracks = remember { mutableStateListOf<VideoTrackInfo>() }
    var currentTrackName by remember { mutableStateOf("Auto") }


    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isLoading = false
                }
                if (playbackState == Player.STATE_BUFFERING) {
                    isLoading = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                errorMessage = "Erreur de lecture: ${error.message}"
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Update available video tracks
                videoTracks.clear()
                var hasVideoTracks = false

                // Add Auto option first
                videoTracks.add(VideoTrackInfo("Auto", null, null))

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val trackFormat = group.getTrackFormat(i)
                            if (group.isTrackSupported(i)) {
                                hasVideoTracks = true
                                val height = trackFormat.height
                                val bitrate = trackFormat.bitrate
                                val name = if (height != -1) "${height}p" else "Inconnu"
                                // Avoid duplicates if possible, though simple list is ok for now
                                videoTracks.add(VideoTrackInfo(name, group.mediaTrackGroup, i, height, bitrate))
                            }
                        }
                    }
                }

                // Determine current selection name
                val parameters = exoPlayer.trackSelectionParameters
                if (parameters.overrides.isEmpty()) {
                    // Auto mode - try to find what's actually playing
                    val width = exoPlayer.videoFormat?.width ?: 0
                    val height = exoPlayer.videoFormat?.height ?: 0
                    currentTrackName = if (height > 0) "Auto (${height}p)" else "Auto"
                } else {
                    // Manual mode
                    // Find the track that matches the override
                   val override = parameters.overrides.values.firstOrNull()
                   if (override != null) {
                       // Find matching track in our list
                       val matching = videoTracks.find {
                           it.group == override.mediaTrackGroup && override.trackIndices.contains(it.trackIndex)
                       }
                       currentTrackName = matching?.name ?: "Manuel"
                   }
                }
            }

            // Listen for video size changes to update Auto label
             override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                 val parameters = exoPlayer.trackSelectionParameters
                 if (parameters.overrides.isEmpty()) {
                     currentTrackName = "Auto (${videoSize.height}p)"
                 }
             }
        }

        exoPlayer.addListener(listener)
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        keepScreenOn = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )

            // Boutons d'overlay (Qualité & Format)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Row {
                    // Bouton Format d'affichage
                    IconButton(
                        onClick = { showResizeDialog = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                         Icon(
                             imageVector = Icons.Default.AspectRatio,
                             contentDescription = "Format",
                             tint = Color.White
                         )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Bouton Qualité
                    IconButton(
                        onClick = { showQualityDialog = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "Qualité",
                            tint = Color.White
                        )
                    }
                }
            }

            if (showResizeDialog) {
                ResizeSelectionDialog(
                    currentMode = resizeMode,
                    onDismiss = { showResizeDialog = false },
                    onModeSelected = { mode ->
                        resizeMode = mode
                        showResizeDialog = false
                    }
                )
            }

            if (showQualityDialog) {
                QualitySelectionDialog(
                    tracks = videoTracks,
                    currentTrackName = currentTrackName,
                    onDismiss = { showQualityDialog = false },
                    onTrackSelected = { trackInfo ->
                        if (trackInfo.group != null && trackInfo.trackIndex != null) {
                            // Manual selection
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(trackInfo.group, trackInfo.trackIndex)
                                )
                                .build()
                            currentTrackName = trackInfo.name
                        } else {
                            // Auto selection
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .build()
                            val height = exoPlayer.videoFormat?.height ?: 0
                            currentTrackName = if (height > 0) "Auto (${height}p)" else "Auto"
                        }
                        showQualityDialog = false
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

data class VideoTrackInfo(
    val name: String,
    val group: TrackGroup?,
    val trackIndex: Int?,
    val height: Int = 0,
    val bitrate: Int = 0
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ResizeSelectionDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit
) {
    val modes = listOf(
        "Ajuster (Fit)" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
        "Remplir (Fill)" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
        "Zoom" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        "Largeur fixe" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        "Hauteur fixe" to AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Format d'affichage") },
        text = {
            LazyColumn {
                items(modes) { (name, mode) ->
                    val isSelected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun QualitySelectionDialog(
    tracks: List<VideoTrackInfo>,
    currentTrackName: String,
    onDismiss: () -> Unit,
    onTrackSelected: (VideoTrackInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Qualité vidéo") },
        text = {
            LazyColumn {
                items(tracks) { track ->
                    val isSelected = if (track.group == null) {
                        currentTrackName.startsWith("Auto")
                    } else {
                        track.name == currentTrackName
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onTrackSelected(track) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (track.group == null && currentTrackName.startsWith("Auto")) currentTrackName else track.name, // Show dynamic Auto label in list if selected
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        Text(text = message, color = Color.White)
    }
}
