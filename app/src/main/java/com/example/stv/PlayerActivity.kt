package com.example.stv

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.stv.security.PermissionHelper

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private val TAG = "PlayerActivity"
    private var isInPipMode by mutableStateOf(false)
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var adManager: AdManager
    private lateinit var adsController: AdsController
    private lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ SÉCURITÉ : Vérifier que l'app appelante est autorisée
        val permissionHelper = PermissionHelper(this)
        val callerPackage = callingPackage
        val requiredPermission = "com.example.stv.PERMISSION_LAUNCH_PLAYER"

        if (callerPackage != null && !permissionHelper.isCallerAuthorized(callerPackage, requiredPermission)) {
            Log.e(TAG, "Unauthorized caller. Finishing activity.")
            finish()
            return
        }

        MobileAds.initialize(this) {}
        adManager = AdManager(this)
        adsController = AdsController(adManager)
        playerController = PlayerController()

        hideSystemUI()

        val videoUrl = intent.getStringExtra("VIDEO_URL")
        val skipAds = intent.getBooleanExtra("SKIP_ADS", false)

        // Validation initiale de l'URL
        val urlError = playerController.validateStreamUrl(videoUrl)
        if (urlError != null) {
            Log.w(TAG, "Invalid URL provided")
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Invalid URL: $urlError")
            }
        }

        setContent {
            STVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    // ✅ Récupérer les ressources en contexte Composable d'abord
                    val errorUrlNotProvided = stringResource(R.string.url_not_provided)

                    // State machine unique pour gérer les différentes phases
                    var uiState by remember {
                        mutableStateOf<PlayerUiState>(
                            if (urlError != null) {
                                PlayerUiState.Error(urlError)
                            } else if (videoUrl == null) {
                                PlayerUiState.Error(errorUrlNotProvided)
                            } else if (skipAds) {
                                PlayerUiState.Ready(videoUrl)
                            } else {
                                PlayerUiState.LoadingAds
                            }
                        )
                    }

                    // Orchestration des ads si besoin
                    LaunchedEffect(uiState) {
                        if (uiState == PlayerUiState.LoadingAds && videoUrl != null) {
                            val result = adsController.showAdIfNeeded(
                                activity = this@PlayerActivity,
                                onAdShowed = {
                                    // ✅ Pub commence à s'afficher → passer en ShowingAd
                                    uiState = PlayerUiState.ShowingAd(videoUrl)
                                },
                                onAdDismissed = {
                                    // ✅ Pub fermée → passer en Ready (démarrage du player)
                                    uiState = PlayerUiState.Ready(videoUrl)
                                },
                                timeoutMs = 6000
                            )

                            // Gérer le résultat initial si pas de pub affichée
                            if (result != AdResult.AdShowed) {
                                uiState = when (result) {
                                    AdResult.AdDismissed -> {
                                        // Pub fermée immédiatement (rare)
                                        PlayerUiState.Ready(videoUrl)
                                    }
                                    AdResult.FallbackBanner,
                                    AdResult.Timeout -> {
                                        // Fallback temporaire (bannière 5s puis player)
                                        PlayerUiState.Fallback(adBlockDetected = false)
                                    }
                                    AdResult.AdBlockDetected -> {
                                        // ✅ BLOQUÉ : Adblock détecté → PAS d'accès au contenu
                                        PlayerUiState.Blocked
                                    }
                                    else -> PlayerUiState.Fallback(adBlockDetected = false)
                                }
                            }
                        }
                    }

                    // Rendu basé sur l'état unique
                    when (uiState) {
                        is PlayerUiState.Error -> {
                            ErrorScreen((uiState as PlayerUiState.Error).message)
                        }
                        is PlayerUiState.LoadingAds -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.Red)
                            }
                        }
                        is PlayerUiState.ShowingAd -> {
                            // ✅ Pub en cours d'affichage
                            // Le player NE DÉMARRE PAS (pas de son en arrière-plan)
                            // Affichage d'un écran noir en attendant la fermeture de la pub
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                // Optionnel : loader discret
                                CircularProgressIndicator(
                                    color = Color.Red.copy(alpha = 0.3f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            // La transition vers Ready se fera via le callback onAdDismissed
                        }
                        is PlayerUiState.Blocked -> {
                            // ✅ BLOQUÉ : Adblock détecté
                            // Le player NE DÉMARRE JAMAIS
                            // Affiche un message et reste bloqué (le dialogue système se charge de fermer l'app)
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
                                        imageVector = Icons.Filled.Block,
                                        contentDescription = "Bloqué",
                                        tint = Color.Red,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Accès bloqué",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Un bloqueur de publicité a été détecté.\nVeuillez le désactiver pour continuer.",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // Pas de transition vers Ready : l'utilisateur reste bloqué
                            // Le dialogue système (AdManager) se charge de fermer l'app
                        }
                        is PlayerUiState.Fallback -> {
                            FallbackBanner(
                                adBlockDetected = (uiState as PlayerUiState.Fallback).adBlockDetected,
                                onFinish = {
                                    if (videoUrl != null) {
                                        uiState = PlayerUiState.Ready(videoUrl)
                                    }
                                }
                            )
                        }
                        is PlayerUiState.Ready -> {
                            val url = (uiState as PlayerUiState.Ready).videoUrl

                            // ✅ Initialize player SEULEMENT ici (après fermeture de la pub)
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
        viewModel.play()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (!isInPipMode) {
            viewModel.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // Continue playing in PIP mode
        } else {
            viewModel.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.pause()
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
                    contentDescription = "Retour",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp) // Légèrement plus grand mais pas trop
                            )
                        }

                        // Forward +10s
                        IconButton(onClick = onForwardClick) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Cast (À implémenter)
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Filled.Cast,
                                contentDescription = "Cast",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Aspect Ratio (Redimensionnement)
                        IconButton(onClick = onResizeClick) {
                            Icon(
                                imageVector = Icons.Filled.AspectRatio,
                                contentDescription = "Format",
                                tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) Color.Red else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Picture in Picture (PiP)
                        IconButton(onClick = onPipClick) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPicture,
                                contentDescription = "PiP",
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

@Composable
fun FallbackBanner(adBlockDetected: Boolean = false, onFinish: () -> Unit) {
    // Affiche la bannière légère pendant 5 secondes pour laisser le temps à la pub bannière de se charger
    var timeLeft by remember { mutableLongStateOf(5L) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (adBlockDetected) "Détection de bloqueur actif" else "Préparation du flux...",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // AdView container for MREC (Medium Rectangle)
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { ctx ->
                    com.google.android.gms.ads.AdView(ctx).apply {
                        setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE)
                        adUnitId = BuildConfig.ADMOB_BANNER_ID  // ✅ Flavor-specific
                        loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Indicateur de progression
            CircularProgressIndicator(
                progress = { (5 - timeLeft) / 5f },
                color = Color.Red,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lancement du flux dans $timeLeft s",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}
