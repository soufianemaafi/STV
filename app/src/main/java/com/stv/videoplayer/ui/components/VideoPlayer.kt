package com.stv.videoplayer.ui.components

import androidx.annotation.OptIn
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.stv.videoplayer.features.player.presentation.PlayerUiAction
import com.stv.videoplayer.features.player.presentation.PlayerViewModel
import kotlinx.coroutines.delay

/** Remonte la chaîne de `ContextWrapper` jusqu'à l'[Activity] hôte. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.isInPictureInPictureCompat(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode

/**
 * Composable principal du player vidÃ©o.
 * Affiche la surface ExoPlayer, l'overlay de contrÃ´les, le dialog qualitÃ© et le loader.
 *
 * ## Cycle de vie du player (Mission 2)
 * Le [Player][androidx.media3.common.Player] appartient exclusivement Ã  [PlayerViewModel]
 * (survit aux rotations). Ce composable ne fait qu'**attacher/dÃ©tacher** une [PlayerView]
 * Ã  cette instance, en suivant le cycle de vie de l'Activity hÃ´te :
 * - `ON_START` â†’ `playerView.player = viewModel.player`
 * - `ON_STOP`  â†’ `playerView.player = null` (empÃªche la vue â€” liÃ©e Ã  l'Activity â€” de
 *   retenir le player aprÃ¨s la destruction de l'Activity pendant qu'il survit dans le ViewModel).
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
    val isSeekable by viewModel.isSeekable.collectAsState()
    val availableSources by viewModel.availableSources.collectAsState()
    val currentSourceIndex by viewModel.currentSourceIndex.collectAsState()

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
        val lifecycleOwner = LocalLifecycleOwner.current
        val activity = LocalContext.current.findActivity()
        val isInPipModeState by rememberUpdatedState(isInPipMode)
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

            // âœ… Attache/dÃ©tache la PlayerView au Player du ViewModel selon le cycle de
            // vie de l'Activity â€” le Player, lui, vit dans le ViewModel et survit aux
            // rotations d'Ã©cran. EmpÃªche toute rÃ©tention de la vue par le player (ou
            // inversement) aprÃ¨s destruction de l'Activity.
        DisposableEffect(lifecycleOwner, viewModel.player) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        playerViewRef.value?.player = viewModel.player
                        // Reprise uniquement si la lecture était active avant ON_STOP.
                        viewModel.onAction(PlayerUiAction.Foreground)
                    }
                    Lifecycle.Event.ON_STOP -> {
                        playerViewRef.value?.player = null
                        // Pause en arrière-plan, sauf Picture-in-Picture.
                        val inPip = isInPipModeState || activity?.isInPictureInPictureCompat() == true
                        viewModel.onAction(PlayerUiAction.Background(isInPictureInPicture = inPip))
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            // Attachement immÃ©diat si le composable apparaÃ®t alors que l'Activity est dÃ©jÃ  dÃ©marrÃ©e
            playerViewRef.value?.player = viewModel.player

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                // âœ… DÃ©tachement final : la vue ne doit jamais retenir le player
                // au-delÃ  de sa propre destruction.
                playerViewRef.value?.player = null
            }
        }

        // Overlay avec les contrÃ´les personnalisÃ©s (masquÃ©s en PiP).
        // En cas d'erreur, le sélecteur de sources reste toujours cliquable.
        if (!isInPipMode) {
            PlayerControls(
                    isVisible = areControlsVisible || errorMessage != null,
                    isPlaying = isPlaying,
                    isLive = isLive,
                    isSeekable = isSeekable,
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
                    onSeek = { viewModel.onAction(PlayerUiAction.SeekTo(it)) },
                    sources = availableSources,
                    currentSourceIndex = currentSourceIndex,
                    onSourceClick = { source, index ->
                        viewModel.onAction(PlayerUiAction.SwitchSource(source.url, index))
                        areControlsVisible = true
                    }
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}


