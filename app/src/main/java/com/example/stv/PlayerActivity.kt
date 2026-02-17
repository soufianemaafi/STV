package com.example.stv

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.stv.ui.theme.STVTheme

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)

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
                        VideoPlayer(videoUrl, isInPipMode)
                    } else {
                        ErrorScreen(stringResource(R.string.url_not_provided))
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        }
        isInPipMode = isInPictureInPictureMode
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, isInPipMode: Boolean, viewModel: PlayerViewModel = viewModel()) {

    // ExoPlayer state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val videoTracks by viewModel.videoTracks.collectAsState()
    val currentTrackName by viewModel.currentTrackName.collectAsState()

    // UI state
    var showQualityDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    LaunchedEffect(url) {
        viewModel.initializePlayer(url)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val exoPlayer = viewModel.exoPlayer
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = !isInPipMode
                            keepScreenOn = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    },
                    update = { playerView ->
                        playerView.resizeMode = resizeMode
                        playerView.useController = !isInPipMode
                        // Important: reconnecter le player si la vue est recréée mais le VM a gardé le player
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Boutons d'overlay (Qualité & Format) - Masqués en mode PiP
            if (!isInPipMode) {
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
                             contentDescription = stringResource(R.string.format_content_description),
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
                            contentDescription = stringResource(R.string.quality_content_description),
                            tint = Color.White
                        )
                    }
                }
            } // Fermeture Box
        } // Fermeture if

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
                        viewModel.selectTrack(trackInfo)
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ResizeSelectionDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit
) {
    val modes = listOf(
        stringResource(R.string.resize_mode_fit) to AspectRatioFrameLayout.RESIZE_MODE_FIT,
        stringResource(R.string.resize_mode_fill) to AspectRatioFrameLayout.RESIZE_MODE_FILL,
        stringResource(R.string.resize_mode_zoom) to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        stringResource(R.string.resize_mode_fixed_width) to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        stringResource(R.string.resize_mode_fixed_height) to AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.resize_dialog_title)) },
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
                Text(stringResource(R.string.close_button))
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
        title = { Text(text = stringResource(R.string.quality_dialog_title)) },
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
                Text(stringResource(R.string.close_button))
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
