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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.example.stv.ads.AdsController
import com.example.stv.ads.AdsController.AdResult
import com.example.stv.player.PlayerController
import com.example.stv.ui.PlayerUiState
import com.example.stv.ui.components.BlockedScreen
import com.example.stv.ui.components.ErrorScreen
import com.example.stv.ui.components.VideoPlayer
import com.example.stv.ui.theme.STVTheme
import kotlinx.coroutines.launch

/**
 * Activity principale du player vidéo.
 * Gère le lifecycle, les ads, la sécurité et délègue l'UI aux composables extraits.
 */
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
        playerController = PlayerController(this)

        hideSystemUI()

        val videoUrl = playerController.resolveVideoUrl(intent)
        val skipAds = intent.getBooleanExtra("SKIP_ADS", false)
        val urlError = playerController.validateStreamUrl(videoUrl)

        // ✅ Initialiser l'état UI IMMÉDIATEMENT (avant setContent)
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
                    LaunchedEffect(Unit) {
                        val state = viewModel.uiState.value
                        if (state is PlayerUiState.LoadingAds) {
                            val url = state.videoUrl
                            launchAdFlow(url)
                        }
                    }

                    // ✅ Rendu UI basé sur l'état — composables extraits
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
                            BlockedScreen(
                                icon = Icons.Filled.Refresh,
                                title = stringResource(R.string.ad_load_failed_title),
                                message = stringResource(R.string.ad_load_failed_message),
                                onRetry = { retryAds() }
                            )
                        }
                        is PlayerUiState.NetworkError -> {
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

        viewModel.releasePlayer()

        val newVideoUrl = playerController.resolveVideoUrl(intent)
        val newSkipAds = intent.getBooleanExtra("SKIP_ADS", false)
        val newUrlError = playerController.validateStreamUrl(newVideoUrl)

        val errorMsg = newUrlError ?: if (newVideoUrl == null) getString(R.string.url_not_provided) else null
        viewModel.initializeUiState(
            videoUrl = newVideoUrl,
            urlError = errorMsg,
            errorUrlNotProvided = getString(R.string.url_not_provided),
            skipAds = newSkipAds
        )

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
