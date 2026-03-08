package com.example.stv.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.stv.PlayerViewModel
import kotlinx.coroutines.delay

/**
 * Composable principal du player vidéo.
 * Affiche la surface ExoPlayer, l'overlay de contrôles, le dialog qualité et le loader.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    isInPipMode: Boolean,
    viewModel: PlayerViewModel = viewModel(),
    onPipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // ExoPlayer state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val videoTracks by viewModel.videoTracks.collectAsState()
    val currentTrackName by viewModel.currentTrackName.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val bufferedPosition by viewModel.bufferedPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isLive by viewModel.isLive.collectAsState()

    // UI state
    var showQualityDialog by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FILL) }
    var areControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(3000)
            areControlsVisible = false
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            areControlsVisible = !areControlsVisible
        }
    ) {
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
                            useController = false
                            keepScreenOn = true
                        }
                    },
                    update = { playerView ->
                        if (playerView.resizeMode != resizeMode) {
                            playerView.resizeMode = resizeMode
                        }
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Overlay avec les contrôles personnalisés (masqués en PiP)
            if (!isInPipMode) {
                PlayerControls(
                    isVisible = areControlsVisible,
                    isPlaying = isPlaying,
                    isLive = isLive,
                    currentPosition = currentPosition,
                    bufferedPosition = bufferedPosition,
                    duration = duration,
                    resizeMode = resizeMode,
                    onBackClick = onBackClick,
                    onRewindClick = {
                        viewModel.seekRewind()
                        areControlsVisible = true
                    },
                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                        areControlsVisible = true
                    },
                    onForwardClick = {
                        viewModel.seekForward()
                        areControlsVisible = true
                    },
                    onResizeClick = {
                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        areControlsVisible = true
                    },
                    onPipClick = onPipClick,
                    onSettingsClick = {
                        showQualityDialog = true
                        areControlsVisible = true
                    },
                    onSeek = { viewModel.seekTo(it) }
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
                    color = Color.Red
                )
            }
        }
    }
}

