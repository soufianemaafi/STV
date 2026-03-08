package com.example.stv

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.stv.ads.AdsController
import com.example.stv.ads.AdsController.AdResult
import com.example.stv.player.PlayerController
import com.example.stv.ui.PlayerUiState
import com.example.stv.ui.theme.STVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

// ✅ PlayerActivity est une Activity normale, pas une API Media3 instable
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    @Suppress("UNUSED")
    private val tag = "PlayerActivity"
    private var isInPipMode by mutableStateOf(false)
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var adManager: AdManager
    private lateinit var adsController: AdsController
    private lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ SÉCURITÉ : Vérifier l'appelant avant de continuer
        val permissionHelper = com.example.stv.security.PermissionHelper(this)
        val callerPackage = callingActivity?.packageName

        if (!permissionHelper.isCallerAuthorizedForPlayer(callerPackage)) {
            Log.w(tag, "Unauthorized caller: $callerPackage. Blocking access.")
            // Afficher un message d'erreur et fermer l'activité
            setContent {
                STVTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = stringResource(R.string.access_denied),
                                    tint = Color.Red,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.access_denied),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.this_app_not_authorized),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { finish() }) {
                                    Text(stringResource(R.string.close_button))
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        // ✅ Utiliser le singleton AdManager (initialisé dans STVApplication)
        adManager = AdManager.instance
        adsController = AdsController(adManager)
        playerController = PlayerController()

        hideSystemUI()

        val videoUrl = playerController.resolveVideoUrl(intent)
        val skipAds = intent.getBooleanExtra("SKIP_ADS", false)
        val urlError = playerController.validateStreamUrl(videoUrl)

        // ✅ Initialiser l'état UI IMMÉDIATEMENT (avant setContent)
        // Cela garantit que le LaunchedEffect des ads verra le bon état dès le premier frame
        viewModel.initializeUiState(
            videoUrl = videoUrl,
            urlError = urlError,
            errorUrlNotProvided = getString(R.string.url_not_provided),
            skipAds = skipAds
        )

        setContent {
            STVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // Tant que l'état n'est pas initialisé → écran noir
                    if (uiState == null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        return@Surface
                    }

                    val currentState = uiState!!

                    // ✅ Flux ads : se lance quand l'état est LoadingAds
                    // Clé = Unit → ne se relance JAMAIS (pas annulé quand l'état change)
                    // Pour relancer : passer par retryAds() qui remet l'état à LoadingAds
                    LaunchedEffect(Unit) {
                        val state = viewModel.uiState.value
                        if (state is PlayerUiState.LoadingAds) {
                            val url = state.videoUrl
                            launchAdFlow(url)
                        }
                    }

                    // ✅ Rendu UI basé sur l'état
                    when (currentState) {
                        is PlayerUiState.Error -> {
                            ErrorScreen(currentState.message)
                        }
                        is PlayerUiState.LoadingAds -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.Red)
                            }
                        }
                        is PlayerUiState.ShowingAd -> {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        }
                        is PlayerUiState.NeedRetry -> {
                            // Pub échouée — bouton Réessayer
                            BlockedScreen(
                                icon = Icons.Filled.Refresh,
                                title = stringResource(R.string.ad_load_failed_title),
                                message = stringResource(R.string.ad_load_failed_message),
                                onRetry = { retryAds() }
                            )
                        }
                        is PlayerUiState.NetworkError -> {
                            // Pas de réseau — bouton Réessayer
                            BlockedScreen(
                                icon = Icons.Filled.WifiOff,
                                title = stringResource(R.string.no_network_title),
                                message = stringResource(R.string.no_network_message),
                                onRetry = { retryAds() }
                            )
                        }
                        is PlayerUiState.Ready -> {
                            val url = currentState.videoUrl

                            LaunchedEffect(url) {
                                viewModel.initializePlayer(url)
                            }

                            VideoPlayer(
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
                                    finishAndRemoveTask()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // 1. Libérer le player actuel
        viewModel.releasePlayer()

        // 2. Résoudre la nouvelle URL
        val newVideoUrl = playerController.resolveVideoUrl(intent)
        val newSkipAds = intent.getBooleanExtra("SKIP_ADS", false)
        val newUrlError = playerController.validateStreamUrl(newVideoUrl)

        // 3. Réinitialiser la state machine avec la nouvelle URL
        val errorMsg = newUrlError ?: if (newVideoUrl == null) "URL not provided" else null
        viewModel.initializeUiState(
            videoUrl = newVideoUrl,
            urlError = errorMsg,
            errorUrlNotProvided = getString(R.string.url_not_provided),
            skipAds = newSkipAds
        )

        // 4. Relancer le flux ads (LaunchedEffect(Unit) ne se relance pas)
        if (newVideoUrl != null && errorMsg == null && !newSkipAds) {
            launchAdFlow(newVideoUrl)
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

    /**
     * Lance le flux ads pour une URL donnée.
     * Appelé depuis LaunchedEffect(Unit) au démarrage et depuis retryAds().
     */
    private fun launchAdFlow(url: String) {
        lifecycleScope.launch {
            val result = adsController.showAdIfNeeded(
                activity = this@PlayerActivity,
                onAdShowed = {
                    viewModel.setUiState(PlayerUiState.ShowingAd(url))
                }
            )

            viewModel.setUiState(when (result) {
                AdResult.AdDismissed -> PlayerUiState.Ready(url)
                AdResult.NoNetwork -> PlayerUiState.NetworkError(url)
                AdResult.NeedRetry -> PlayerUiState.NeedRetry(url)
                AdResult.Failed -> PlayerUiState.NeedRetry(url)
            })
        }
    }

    /**
     * Bouton "Réessayer" : remet l'état à LoadingAds et relance le flux ads.
     */
    private fun retryAds() {
        val url = extractCurrentUrl() ?: return
        viewModel.setUiState(PlayerUiState.LoadingAds(url))
        launchAdFlow(url)
    }

    /**
     * Extrait l'URL depuis l'état actuel, quel que soit l'état.
     */
    private fun extractCurrentUrl(): String? {
        return when (val state = viewModel.uiState.value) {
            is PlayerUiState.LoadingAds -> state.videoUrl
            is PlayerUiState.ShowingAd -> state.videoUrl
            is PlayerUiState.Ready -> state.videoUrl
            is PlayerUiState.NetworkError -> state.videoUrl
            is PlayerUiState.NeedRetry -> state.videoUrl
            else -> playerController.resolveVideoUrl(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.exoPlayer != null && viewModel.isReady()) {
            viewModel.play()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (!isInPipMode && viewModel.exoPlayer != null && viewModel.isReady()) {
            viewModel.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isInPictureInPictureMode) {
            // Continue playing in PIP mode
        } else if (viewModel.exoPlayer != null) {
            viewModel.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.exoPlayer != null) {
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
    // Le player démarre en format fill par défaut
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
                    color = Color.Red // Loader rouge
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isLive: Boolean = false,
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    resizeMode: Int,
    onBackClick: () -> Unit,
    onRewindClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onPipClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
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
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // ✅ Badge LIVE en haut à droite (visible uniquement pour les flux en direct)
            if (isLive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    color = Color.Red,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.live_badge),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

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
                        IconButton(onClick = onRewindClick) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = stringResource(R.string.rewind_10s),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause_button) else stringResource(R.string.play_stream_button),
                                tint = Color.White,
                                modifier = Modifier.size(40.dp) // Légèrement plus grand mais pas trop
                            )
                        }

                        // Forward +10s
                        IconButton(onClick = onForwardClick) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = stringResource(R.string.forward_10s),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Cast (À implémenter)
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Filled.Cast,
                                contentDescription = stringResource(R.string.cast_button),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Aspect Ratio (Redimensionnement)
                        IconButton(onClick = onResizeClick) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = stringResource(R.string.format_content_description),
                                tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) Color.Red else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Picture in Picture (PiP)
                        IconButton(onClick = onPipClick) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPicture,
                                contentDescription = stringResource(R.string.pip_button),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Settings (Qualité) - Déplacé à la fin
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
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
                            onValueChange = { onSeek(it.toLong()) },
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

/**
 * Écran bloqué réutilisable : adblock détecté ou pas de réseau.
 * Affiche une icône, un titre, un message et un bouton "Réessayer".
 */
@Composable
fun BlockedScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = stringResource(R.string.retry_button),
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

