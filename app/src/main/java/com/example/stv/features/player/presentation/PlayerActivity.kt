package com.example.stv.features.player.presentation

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
import com.example.stv.AdManager
import com.example.stv.R
import com.example.stv.features.ads.AdFlowController
import com.example.stv.features.ads.AdFlowController.AdResult
import com.example.stv.features.ads.AdFrequencyManager
import com.example.stv.core.security.IntentSecurityManager
import com.example.stv.core.security.IntentValidationResult
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
    private lateinit var adsController: AdFlowController
    // ✅ Sécurité : valide TOUT intent entrant avant de le transmettre au ViewModel
    // (protection Intent Hijacking / Intent Redirection — voir core.security.IntentSecurityManager)
    private val intentSecurityManager = IntentSecurityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Gestion du bouton Retour (Android 13+ : onBackPressedDispatcher)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })

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
        adsController = AdFlowController(adManager, AdFrequencyManager.shared())

        hideSystemUI()

        // ✅ SÉCURITÉ : l'Activity ne fait AUCUNE logique métier — elle délègue
        // la validation stricte de l'Intent (schéma http/https uniquement) au
        // IntentSecurityManager, puis transforme le résultat en action MVI.
        handleIncomingIntent(intent)

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
                    LaunchedEffect(currentState) {
                        if (currentState is PlayerUiState.LoadingAds) {
                            launchAdFlow(
                                url = currentState.videoUrl,
                                bypassCooldown = currentState.forceAdCheck
                            )
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

                            // ✅ MVI : la construction du MediaItem + prepare() est déléguée
                            // au ViewModel via une action, jamais appelée impérativement.
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

        // ✅ Le player (propriété du ViewModel) n'est PAS libéré ici : seule l'URL
        // change. `preparePlayback` (déclenché via PreparePlayback) réutilise
        // l'instance existante — pas de recréation, pas de fuite mémoire.
        handleIncomingIntent(intent)
    }

    /**
     * Point d'entrée unique de traitement des Intents entrants (onCreate + onNewIntent).
     *
     * Aucune logique métier ici : on délègue au [IntentSecurityManager] la validation
     * stricte (schéma http/https uniquement, garde-fous null-safe), puis on transforme
     * le résultat en action MVI envoyée au ViewModel. Si l'intent est invalide ou
     * potentiellement malveillant, il est ignoré en toute sécurité (log + état d'erreur
     * générique) — jamais de crash.
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
                Log.w(tag, "Intent rejeté par IntentSecurityManager: ${result.reason}")
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
     * Lance le flux ads pour une URL donnée.
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
        // ✅ MVI : contrôle du player exclusivement via onAction (jamais d'accès direct)
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
        // ✅ PiP automatique UNIQUEMENT si le player est en lecture (pas pendant les ads, erreurs, etc.)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
            && viewModel.isReady()
            && viewModel.isPlaying.value
        ) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }
}
