package com.stv.videoplayer.features.player.presentation

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import com.stv.videoplayer.AdManager
import com.stv.videoplayer.R
import com.stv.videoplayer.features.ads.AdFlowController
import com.stv.videoplayer.features.ads.AdFlowController.AdResult
import com.stv.videoplayer.features.ads.AdFrequencyManager
import com.stv.videoplayer.core.security.IntentSecurityManager
import com.stv.videoplayer.core.security.IntentValidationResult
import com.stv.videoplayer.ui.components.BlockedScreen
import com.stv.videoplayer.ui.components.ErrorScreen
import com.stv.videoplayer.ui.components.VideoPlayer
import com.stv.videoplayer.ui.theme.STVTheme
import kotlinx.coroutines.launch

/**
 * Activity principale du player vidÃ©o.
 * GÃ¨re le lifecycle, les ads, la sÃ©curitÃ© et dÃ©lÃ¨gue l'UI aux composables extraits.
 */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    @Suppress("UNUSED")
    private val tag = "PlayerActivity"
    private var isInPipMode by mutableStateOf(false)
    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var adManager: AdManager
    private lateinit var adsController: AdFlowController
    // âœ… SÃ©curitÃ© : valide TOUT intent entrant avant de le transmettre au ViewModel
    // (protection Intent Hijacking / Intent Redirection â€” voir core.security.IntentSecurityManager)
    private val intentSecurityManager = IntentSecurityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // âœ… Gestion du bouton Retour (Android 13+ : onBackPressedDispatcher)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })

        // âœ… SÃ‰CURITÃ‰ : VÃ©rifier l'appelant avant de continuer
        val permissionHelper = com.stv.videoplayer.security.PermissionHelper(this)
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

        // âœ… Utiliser le singleton AdManager (initialisÃ© dans STVApplication)
        adManager = AdManager.instance
        adsController = AdFlowController(adManager, AdFrequencyManager.shared())

        hideSystemUI()

        // âœ… SÃ‰CURITÃ‰ : l'Activity ne fait AUCUNE logique mÃ©tier â€” elle dÃ©lÃ¨gue
        // la validation stricte de l'Intent (schÃ©ma http/https uniquement) au
        // IntentSecurityManager, puis transforme le rÃ©sultat en action MVI.
        handleIncomingIntent(intent)

        setContent {
            STVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    // Tant que l'Ã©tat n'est pas initialisÃ© â†’ Ã©cran noir
                    if (uiState == null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        return@Surface
                    }

                    val currentState = uiState!!

                    // âœ… Flux ads : se lance quand l'Ã©tat est LoadingAds
                    LaunchedEffect(currentState) {
                        if (currentState is PlayerUiState.LoadingAds) {
                            launchAdFlow(
                                url = currentState.videoUrl,
                                bypassCooldown = currentState.forceAdCheck
                            )
                        }
                    }

                    // âœ… Rendu UI basÃ© sur l'Ã©tat â€” composables extraits
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
                        is PlayerUiState.AdBlockerBlocked -> {
                            BlockedScreen(
                                icon = Icons.Filled.Refresh,
                                title = stringResource(R.string.ad_load_failed_title),
                                message = stringResource(R.string.ad_load_failed_message),
                                onRetry = { viewModel.onAction(PlayerUiAction.RetryAdCheck) }
                            )
                        }
                        is PlayerUiState.NeedRetry -> {
                            BlockedScreen(
                                icon = Icons.Filled.Refresh,
                                title = stringResource(R.string.ad_load_failed_title),
                                message = stringResource(R.string.ad_load_failed_message),
                                onRetry = { viewModel.onAction(PlayerUiAction.RetryAdCheck) }
                            )
                        }
                        is PlayerUiState.NetworkError -> {
                            BlockedScreen(
                                icon = Icons.Filled.WifiOff,
                                title = stringResource(R.string.no_network_title),
                                message = stringResource(R.string.no_network_message),
                                onRetry = { viewModel.onAction(PlayerUiAction.RetryAdCheck) }
                            )
                        }
                        is PlayerUiState.Ready -> {
                            val url = currentState.videoUrl

                            // âœ… MVI : la construction du MediaItem + prepare() est dÃ©lÃ©guÃ©e
                            // au ViewModel via une action, jamais appelÃ©e impÃ©rativement.
                            LaunchedEffect(url) {
                                viewModel.onAction(PlayerUiAction.PreparePlayback(url))
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

        // âœ… Le player (propriÃ©tÃ© du ViewModel) n'est PAS libÃ©rÃ© ici : seule l'URL
        // change. `preparePlayback` (dÃ©clenchÃ© via PreparePlayback) rÃ©utilise
        // l'instance existante â€” pas de recrÃ©ation, pas de fuite mÃ©moire.
        handleIncomingIntent(intent)
    }

    /**
     * Point d'entrÃ©e unique de traitement des Intents entrants (onCreate + onNewIntent).
     *
     * Aucune logique mÃ©tier ici : on dÃ©lÃ¨gue au [IntentSecurityManager] la validation
     * stricte (schÃ©ma http/https uniquement, garde-fous null-safe), puis on transforme
     * le rÃ©sultat en action MVI envoyÃ©e au ViewModel. Si l'intent est invalide ou
     * potentiellement malveillant, il est ignorÃ© en toute sÃ©curitÃ© (log + Ã©tat d'erreur
     * gÃ©nÃ©rique) â€” jamais de crash.
     */
    private fun handleIncomingIntent(intent: Intent) {
        val skipAds = intent.getBooleanExtra("SKIP_ADS", false)
        val playbackMetadata = extractIncomingPlaybackMetadata(intent)

        when (val result = intentSecurityManager.validateIncomingIntent(intent)) {
            is IntentValidationResult.Valid -> {
                viewModel.onAction(
                    PlayerUiAction.LoadVideo(
                        videoUrl = result.videoUrl,
                        skipAds = skipAds,
                        title = playbackMetadata.title,
                        headers = playbackMetadata.headers
                    )
                )
            }
            is IntentValidationResult.Invalid -> {
                Log.w(tag, "Intent rejetÃ© par IntentSecurityManager: ${result.reason}")
                viewModel.onAction(
                    PlayerUiAction.RejectInvalidIntent(getString(R.string.error_intent_security_rejected))
                )
            }
        }
    }

    private data class IncomingPlaybackMetadata(
        val title: String?,
        val headers: Map<String, String>
    )

    private fun extractIncomingPlaybackMetadata(intent: Intent): IncomingPlaybackMetadata {
        return IncomingPlaybackMetadata(
            title = extractIncomingTitle(intent),
            headers = extractIncomingHeaders(intent)
        )
    }

    private fun extractIncomingTitle(intent: Intent): String? {
        return firstNonBlankText(
            readIntentTextExtra(intent, "title"),
            readIntentTextExtra(intent, Intent.EXTRA_TITLE)
        )
    }

    private fun extractIncomingHeaders(intent: Intent): Map<String, String> {
        val result = linkedMapOf<String, String>()

        val extras = try {
            intent.extras
        } catch (_: Exception) {
            null
        }

        try {
            when (val rawHeaders = readBundleValue(extras, "headers")) {
                is Bundle -> {
                    rawHeaders.keySet().forEach { key ->
                        putCleanHeader(result, key, readBundleValue(rawHeaders, key)?.toString())
                    }
                }
                is Map<*, *> -> {
                    rawHeaders.forEach { (key, value) ->
                        putCleanHeader(result, key?.toString(), value?.toString())
                    }
                }
                is Array<*> -> {
                    rawHeaders.forEach { item ->
                        parseHeaderEntry(item?.toString())?.let { (key, value) ->
                            putCleanHeader(result, key, value)
                        }
                    }
                }
                is Iterable<*> -> {
                    rawHeaders.forEach { item ->
                        parseHeaderEntry(item?.toString())?.let { (key, value) ->
                            putCleanHeader(result, key, value)
                        }
                    }
                }
                is String -> {
                    parseHeaderEntry(rawHeaders)?.let { (key, value) ->
                        putCleanHeader(result, key, value)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Impossible d'extraire les headers HTTP depuis l'Intent", e)
        }

        putCleanHeader(result, "Referer", readIntentTextExtra(intent, "referer"))
        putCleanHeader(result, "Referer", readIntentTextExtra(intent, "Referer"))

        return result.toMap()
    }

    private fun readIntentTextExtra(intent: Intent, key: String): String? {
        return try {
            readBundleValue(intent.extras, key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun readBundleValue(bundle: Bundle?, key: String): Any? {
        if (bundle == null) return null

        return try {
            val method = Bundle::class.java.getMethod("get", String::class.java)
            method.invoke(bundle, key)
        } catch (_: Exception) {
            null
        }
    }

    private fun firstNonBlankText(vararg candidates: String?): String? {
        return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun parseHeaderEntry(raw: String?): Pair<String, String>? {
        val safeRaw = raw?.replace("\r", " ")?.replace("\n", " ")?.trim().orEmpty()
        if (safeRaw.isBlank()) return null

        val separatorIndex = when {
            safeRaw.contains('=') -> safeRaw.indexOf('=')
            safeRaw.contains(':') -> safeRaw.indexOf(':')
            else -> -1
        }

        if (separatorIndex <= 0 || separatorIndex >= safeRaw.lastIndex) return null

        val key = safeRaw.substring(0, separatorIndex).trim()
        val value = safeRaw.substring(separatorIndex + 1).trim()
        if (key.isBlank() || value.isBlank()) return null

        return key to value
    }

    private fun putCleanHeader(target: MutableMap<String, String>, rawKey: String?, rawValue: String?) {
        val key = canonicalizeHeaderName(rawKey)
        val value = cleanHeaderValue(rawValue)
        if (key != null && value != null) {
            target[key] = value
        }
    }

    private fun canonicalizeHeaderName(rawKey: String?): String? {
        val safeKey = rawKey?.replace("\r", " ")?.replace("\n", " ")?.trim().orEmpty()
        if (safeKey.isBlank()) return null

        return when (safeKey.lowercase()) {
            "referer" -> "Referer"
            "user-agent" -> "User-Agent"
            else -> safeKey
        }
    }

    private fun cleanHeaderValue(rawValue: String?): String? {
        return rawValue
            ?.replace("\r", " ")
            ?.replace("\n", " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
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
     * Lance le flux ads pour une URL donnÃ©e.
     */
    private fun launchAdFlow(url: String, bypassCooldown: Boolean = false) {
        lifecycleScope.launch {
            val result = adsController.showAdIfNeeded(
                activity = this@PlayerActivity,
                bypassCooldown = bypassCooldown,
                onAdShowed = {
                    viewModel.setUiState(PlayerUiState.ShowingAd(url))
                }
            )

            viewModel.setUiState(when (result) {
                AdResult.AdDismissed -> PlayerUiState.Ready(url)
                AdResult.SkippedByCooldown -> PlayerUiState.Ready(url)
                AdResult.NoNetwork -> PlayerUiState.NetworkError(url)
                AdResult.AdBlockerBlocked -> PlayerUiState.AdBlockerBlocked(url)
                AdResult.Failed -> PlayerUiState.AdBlockerBlocked(url)
            })
        }
    }

    override fun onStart() {
        super.onStart()
        // âœ… MVI : contrÃ´le du player exclusivement via onAction (jamais d'accÃ¨s direct)
        if (viewModel.isReady()) {
            viewModel.onAction(PlayerUiAction.Play)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (!isInPipMode && viewModel.isReady()) {
            viewModel.onAction(PlayerUiAction.Play)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isInPictureInPictureMode) {
            // Continue playing in PIP mode
        } else {
            viewModel.onAction(PlayerUiAction.Pause)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAction(PlayerUiAction.Pause)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // âœ… PiP automatique UNIQUEMENT si le player est en lecture (pas pendant les ads, erreurs, etc.)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && viewModel.isReady()
            && viewModel.isPlaying.value
        ) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
}

