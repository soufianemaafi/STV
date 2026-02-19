package com.example.stv

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.stv.ui.theme.STVTheme
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    // Instanciation du ViewModel au niveau de l'activité pour gérer le cycle de vie
    private val viewModel: PlayerViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newVideoUrl = intent?.getStringExtra("VIDEO_URL")
        if (newVideoUrl != null) {
            viewModel.initializePlayer(newVideoUrl)
        }
    }

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
                        // On passe le viewModel existant au Composable
                        VideoPlayer(
                            url = videoUrl,
                            isInPipMode = isInPipMode,
                            viewModel = viewModel,
                            onPipClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    enterPictureInPictureMode(
                                        android.app.PictureInPictureParams.Builder().build()
                                    )
                                }
                            },
                            onBackClick = {
                                finish()
                            }
                        )
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

    override fun onStart() {
        super.onStart()
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            viewModel.play()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if ((android.os.Build.VERSION.SDK_INT < 24 || !isInPipMode)) {
            viewModel.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (android.os.Build.VERSION.SDK_INT < 24) {
             viewModel.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (android.os.Build.VERSION.SDK_INT >= 24) {
             viewModel.pause()
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
fun VideoPlayer(
    url: String,
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
    val bufferedPosition by viewModel.bufferedPosition.collectAsState() // Récupérer la position du buffer
    val duration by viewModel.duration.collectAsState()

    // UI state
    var showQualityDialog by remember { mutableStateOf(false) }
    // Le player demarre en format fill comme par defaut
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FILL) }
    var areControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(3000)
            areControlsVisible = false
        }
    }

    LaunchedEffect(url) {
        viewModel.initializePlayer(url)
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
                            useController = false // Désactiver les contrôles natifs
                            keepScreenOn = true
                        }
                    },
                    update = { playerView ->
                        // Force la mise à jour du mode de redimensionnement
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

            // Overlay avec les contrôles personnalisés (Visible seulement si !isInPipMode)
            if (!isInPipMode) {
                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        // Bouton Retour en haut à gauche
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Contrôles centrés (non, on veut en bas selon la demande)
                        // Correction: L'utilisateur a demandé Play/Pause aligné en bas.

                        // Zone du bas (Contrôles complets)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp) // Réduit le padding du bas pour descendre la barre
                                .fillMaxWidth()
                        ) {
                            // Boutons principaux alignés en bas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center, // Changé de SpaceEvenly à Center pour rapprocher les éléments
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Contrôles centralisés pour les rapprocher
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp), // Espace fixe et égal entre les boutons
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rewind -10s
                                    IconButton(onClick = {
                                        viewModel.seekRewind()
                                        areControlsVisible = true // Reset timer
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    // Play/Pause
                                    IconButton(onClick = {
                                        viewModel.togglePlayPause()
                                        areControlsVisible = true // Reset timer
                                    }) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp) // Légèrement plus grand mais pas trop
                                        )
                                    }

                                    // Forward +10s
                                    IconButton(onClick = {
                                        viewModel.seekForward()
                                        areControlsVisible = true // Reset timer
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    // Cast (Placeholder)
                                    IconButton(onClick = { /* TODO: Implémenter Cast */ }) {
                                        Icon(
                                            imageVector = Icons.Default.Cast,
                                            contentDescription = "Cast",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Aspect Ratio (Redimensionnement)
                                    IconButton(onClick = {
                                        resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        } else {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                        areControlsVisible = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = "Format", // stringResource removed to avoid error if not present, simple string is fine
                                            tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) Color.Red else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Picture in Picture (PiP)
                                    IconButton(onClick = {
                                        onPipClick()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.PictureInPicture,
                                            contentDescription = "PiP",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Settings (Qualité) - Déplacé à la fin
                                    IconButton(onClick = {
                                        showQualityDialog = true
                                        areControlsVisible = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.quality_content_description),
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp)) // Réduit l'espace entre les boutons et la barre

                            // Barre de progression et temps
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatDuration(currentPosition),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )

                                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                                    // Barre de buffer (arrière-plan)
                                    LinearProgressIndicator(
                                        progress = { if (duration > 0) bufferedPosition.toFloat() / duration.toFloat() else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp), // Hauteur proche de la track du slider
                                        color = Color.White.copy(alpha = 0.5f), // Couleur du buffer (blanc semi-transparent)
                                        trackColor = Color.White.copy(alpha = 0.2f), // Couleur du fond inactif
                                    )

                                    // Slider de lecture (avant-plan)
                                    Slider(
                                        value = currentPosition.toFloat(),
                                        onValueChange = { viewModel.seekTo(it.toLong()) },
                                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Red, // Curseur rouge
                                            activeTrackColor = Color.Red, // Barre prog rouge
                                            inactiveTrackColor = Color.Transparent // Fond transparent pour voir le buffer derrière
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text(
                                    text = formatDuration(duration),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
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
                    color = Color.Red // Loader rouge
                )
            }
        }
    }
}

// Utilitaire de formatage temps
fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

// ResizeSelectionDialog removed

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
