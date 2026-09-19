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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.stv.features.player.presentation.PlayerUiAction
import com.example.stv.features.player.presentation.PlayerViewModel
import kotlinx.coroutines.delay

/**
 * Composable principal du player vidéo.
 * Affiche la surface ExoPlayer, l'overlay de contrôles, le dialog qualité et le loader.
 *
 * ## Cycle de vie du player (Mission 2)
 * Le [Player][androidx.media3.common.Player] appartient exclusivement à [PlayerViewModel]
 * (survit aux rotations). Ce composable ne fait qu'**attacher/détacher** une [PlayerView]
 * à cette instance, en suivant le cycle de vie de l'Activity hôte :
 * - `ON_START` → `playerView.player = viewModel.player`
 * - `ON_STOP`  → `playerView.player = null` (empêche la vue — liée à l'Activity — de
 *   retenir le player après la destruction de l'Activity pendant qu'il survit dans le ViewModel).
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
            val lifecycleOwner = LocalLifecycleOwner.current
            val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        keepScreenOn = true
                        playerViewRef.value = this
                    }
                },
                update = { playerView ->
                    if (playerView.resizeMode != resizeMode) {
                        playerView.resizeMode = resizeMode
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ✅ Attache/détache la PlayerView au Player du ViewModel selon le cycle de
            // vie de l'Activity — le Player, lui, vit dans le ViewModel et survit aux
            // rotations d'écran. Empêche toute rétention de la vue par le player (ou
            // inversement) après destruction de l'Activity.
            DisposableEffect(lifecycleOwner, viewModel.player) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> playerViewRef.value?.player = viewModel.player
                        Lifecycle.Event.ON_STOP -> playerViewRef.value?.player = null
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                // Attachement immédiat si le composable apparaît alors que l'Activity est déjà démarrée
                playerViewRef.value?.player = viewModel.player

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    // ✅ Détachement final : la vue ne doit jamais retenir le player
                    // au-delà de sa propre destruction.
                    playerViewRef.value?.player = null
                }
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
                        viewModel.onAction(PlayerUiAction.SeekRewind)
                        areControlsVisible = true
                    },
                    onPlayPauseClick = {
                        viewModel.onAction(PlayerUiAction.TogglePlayPause)
                        areControlsVisible = true
                    },
                    onForwardClick = {
                        viewModel.onAction(PlayerUiAction.SeekForward)
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
                    onSeek = { viewModel.onAction(PlayerUiAction.SeekTo(it)) }
                )
            }

            if (showQualityDialog) {
                QualitySelectionDialog(
                    tracks = videoTracks,
                    currentTrackName = currentTrackName,
                    onDismiss = { showQualityDialog = false },
                    onTrackSelected = { trackInfo ->
                        viewModel.onAction(PlayerUiAction.SelectTrack(trackInfo))
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

