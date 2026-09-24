package com.stv.videoplayer.features.player.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.stv.videoplayer.R
import com.stv.videoplayer.core.domain.model.VideoTrackInfo
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel MVI du player vidÃ©o â€” **seul et unique propriÃ©taire** du cycle de vie d'ExoPlayer.
 *
 * ## ðŸ”’ SÃ©curitÃ© mÃ©moire (Mission 2 â€” isolation Media3 dans la couche Presentation)
 * - Le [ExoPlayer] est construit **une seule fois**, avec le contexte de l'[Application]
 *   (jamais celui d'une Activity) via [playerFactory]/[createDefaultExoPlayer] â†’ aucune
 *   fuite mÃ©moire possible liÃ©e Ã  la rÃ©tention d'une rÃ©fÃ©rence Activity aprÃ¨s sa destruction.
 * - Ce ViewModel (`AndroidViewModel`) **survit aux rotations d'Ã©cran** (l'Activity est
 *   recrÃ©Ã©e, pas le ViewModel) : le player n'est donc JAMAIS recrÃ©Ã© lors d'une rotation,
 *   la lecture continue sans interruption.
 * - [onCleared] appelle **IMPÃ‰RATIVEMENT** `player.release()` quand le ViewModel est
 *   dÃ©finitivement dÃ©truit (Activity `finish()`/back) â€” zÃ©ro fuite mÃ©moire garantie.
 * - L'Activity/UI (`PlayerActivity` / composable `VideoPlayer`) ne fait qu'**attacher /
 *   dÃ©tacher** une `PlayerView` Ã  [player] (`playerView.player = viewModel.player` puis
 *   `= null`) â€” elle ne possÃ¨de ni ne contrÃ´le jamais l'instance du player directement ;
 *   tout contrÃ´le passe par [onAction] (MVI).
 *
 * @param playerFactory Factory de construction du player, injectable pour les tests
 * unitaires (permet d'injecter un [Player] mockÃ© â€” interface pure â€” plutÃ´t qu'un
 * [ExoPlayer] concret qui nÃ©cessite un environnement Android rÃ©el). Par dÃ©faut :
 * [createDefaultExoPlayer], qui utilise TOUJOURS le contexte Application.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel @JvmOverloads constructor(
    application: Application,
    private val playerFactory: (Application) -> Player = ::createDefaultExoPlayer
) : AndroidViewModel(application) {

    // âœ… SÃ‰CURITÃ‰ MÃ‰MOIRE : construit UNE SEULE FOIS avec le contexte Application.
    // Ne JAMAIS reconstruire avec un contexte d'Activity (fuite mÃ©moire garantie).
    // TypÃ© [Player] (interface Media3) â€” seules les capacitÃ©s de contrÃ´le de lecture
    // communes sont utilisÃ©es ici, ce qui permet aussi de tester ce ViewModel avec un
    // Player mockÃ© sans dÃ©pendre de la classe concrÃ¨te ExoPlayer (native/Android).
    private val _player: Player = playerFactory(application)

    /**
     * Instance exposÃ©e Ã  l'UI. L'UI ne doit s'en servir QUE pour l'attacher/dÃ©tacher
     * d'une `PlayerView` (`playerView.player = viewModel.player`) â€” tout contrÃ´le
     * (play/pause/seek/...) doit passer par [onAction].
     */
    val player: Player get() = _player

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _videoTracks = MutableStateFlow<List<VideoTrackInfo>>(emptyList())
    val videoTracks: StateFlow<List<VideoTrackInfo>> = _videoTracks.asStateFlow()

    private val _currentTrackName = MutableStateFlow(getApplication<Application>().getString(R.string.quality_auto))
    val currentTrackName: StateFlow<String> = _currentTrackName.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    @Suppress("unused")
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    // âœ… DÃ©tection flux LIVE (HLS live, DASH live, etc.)
    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    // State machine : null = pas encore initialisÃ© (attend une PlayerUiAction)
    private val _uiState = MutableStateFlow<PlayerUiState?>(null)
    val uiState: StateFlow<PlayerUiState?> = _uiState.asStateFlow()

    private var currentUrl: String? = null

    // âœ… DerniÃ¨re hauteur vidÃ©o connue (via onVideoSizeChanged), utilisÃ©e pour afficher
    // "Auto (1080p)" sans dÃ©pendre de `ExoPlayer.videoFormat` (spÃ©cifique Ã  la classe
    // concrÃ¨te) â€” garde `_player` typÃ© [Player] uniquement (interface pure, testable).
    private var lastVideoHeight: Int = 0

    // âœ… Job explicite pour annuler la coroutine de position dans onCleared (pas de fuite de coroutine)
    private var positionUpdateJob: Job? = null

    /**
     * Synchronise le `StateFlow<PlayerUiState>` (et les autres StateFlows) avec les
     * Ã©vÃ©nements rÃ©els du player Media3. AjoutÃ© une seule fois, Ã  la crÃ©ation du ViewModel.
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _isLoading.value = true
                Player.STATE_READY -> {
                    _isLoading.value = false
                    _duration.value = _player.duration.coerceAtLeast(0L)
                    _isLive.value = _player.isCurrentMediaItemLive
                }
                Player.STATE_ENDED -> {
                    _isLoading.value = false
                    _isPlaying.value = false
                }
                Player.STATE_IDLE -> _isLoading.value = false
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            _errorMessage.value = getUserFriendlyErrorMessage(error)
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateVideoTracks(tracks)
            updateCurrentTrackName()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            lastVideoHeight = videoSize.height
            updateCurrentTrackName()
        }
    }

    init {
        _player.addListener(playerListener)
        startPositionUpdates()
    }

    /**
     * Point d'entrÃ©e MVI **unique** pour la View. Aucune logique mÃ©tier cÃ´tÃ©
     * Activity/Composable : tout contrÃ´le du player passe par ici.
     *
     * AppelÃ© depuis `PlayerActivity` (`onCreate`/`onNewIntent`) et depuis le
     * composable `VideoPlayer` (contrÃ´les play/pause/seek/qualitÃ©).
     */
    fun onAction(action: PlayerUiAction) {
        when (action) {
            is PlayerUiAction.LoadVideo -> {
                currentUrl = action.videoUrl
                _currentTitle.value = action.title?.trim()?.takeIf { it.isNotEmpty() }
                pendingRequestHeaders = cleanHeaders(action.headers)
                _uiState.value = if (action.skipAds) {
                    PlayerUiState.Ready(action.videoUrl)
                } else {
                    PlayerUiState.LoadingAds(action.videoUrl)
                }
            }
            is PlayerUiAction.RetryAdCheck -> retryAdCheck()
            is PlayerUiAction.RejectInvalidIntent -> {
                _uiState.value = PlayerUiState.Error(action.userMessage)
            }
            is PlayerUiAction.PreparePlayback -> preparePlayback(action.videoUrl)
            is PlayerUiAction.Play -> _player.play()
            is PlayerUiAction.Pause -> _player.pause()
            is PlayerUiAction.TogglePlayPause -> togglePlayPause()
            is PlayerUiAction.SeekTo -> seekTo(action.positionMs)
            is PlayerUiAction.SeekForward -> seekForward()
            is PlayerUiAction.SeekRewind -> seekRewind()
            is PlayerUiAction.SelectTrack -> selectTrack(action.trackInfo)
        }
    }

    /**
     * Met Ã  jour l'Ã©tat UI directement. UtilisÃ© par `PlayerActivity` pour les
     * transitions liÃ©es au flux publicitaire (ads dismissed/failed/network error),
     * qui restent orchestrÃ©es cÃ´tÃ© Activity via `AdsController` (hors scope Media3).
     */
    fun setUiState(newState: PlayerUiState) {
        _uiState.value = newState
    }

    /** VÃ©rifie si l'Ã©tat actuel est Ready */
    fun isReady(): Boolean = _uiState.value is PlayerUiState.Ready

    private fun getUserFriendlyErrorMessage(error: PlaybackException): String {
        val app = getApplication<Application>()
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> app.getString(R.string.error_network)
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> app.getString(R.string.error_content_not_found)
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> app.getString(R.string.error_decoder)
            else -> app.getString(R.string.error_unknown, error.localizedMessage ?: "Unknown")
        }
    }

    /**
     * Construit le [MediaItem] et lance la prÃ©paration du player : `setMediaItem` + `prepare()`.
     * N'est appelÃ© qu'une seule fois par URL (idempotent) â€” une rotation d'Ã©cran ne
     * relance donc jamais la prÃ©paration puisque le ViewModel (et `currentUrl`) survit.
     */
    private fun preparePlayback(url: String) {
        if (currentUrl == url && _player.playbackState != Player.STATE_IDLE) return

        currentUrl = url
        _isLoading.value = true
        _errorMessage.value = null

        _player.playWhenReady = true
        val mediaItemBuilder = MediaItem.Builder().setUri(url)
        _currentTitle.value?.takeIf { it.isNotBlank() }?.let { title ->
            mediaItemBuilder.setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build()
            )
        }
        _player.setMediaItem(mediaItemBuilder.build())
        _player.prepare()
    }

    private fun retryAdCheck() {
        val url = currentUrl ?: extractCurrentUrlFromState() ?: return
        currentUrl = url
        _errorMessage.value = null
        _uiState.value = PlayerUiState.LoadingAds(url, forceAdCheck = true)
    }

    private fun extractCurrentUrlFromState(): String? {
        return when (val state = _uiState.value) {
            is PlayerUiState.LoadingAds -> state.videoUrl
            is PlayerUiState.ShowingAd -> state.videoUrl
            is PlayerUiState.Ready -> state.videoUrl
            is PlayerUiState.NeedRetry -> state.videoUrl
            is PlayerUiState.NetworkError -> state.videoUrl
            is PlayerUiState.AdBlockerBlocked -> state.videoUrl
            else -> null
        }
    }

    private fun togglePlayPause() {
        if (_player.isPlaying) _player.pause() else _player.play()
    }

    private fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _player.duration.coerceAtLeast(0L))
        _player.seekTo(clamped)
        _currentPosition.value = clamped
    }

    private fun seekForward() {
        seekTo((_player.currentPosition + 10_000).coerceAtMost(_player.duration.coerceAtLeast(0L)))
    }

    private fun seekRewind() {
        seekTo((_player.currentPosition - 10_000).coerceAtLeast(0L))
    }

    private fun selectTrack(trackInfo: VideoTrackInfo) {
        if (trackInfo.group != null && trackInfo.trackIndex != null) {
            // SÃ©lection manuelle
            _player.trackSelectionParameters = _player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(trackInfo.group, trackInfo.trackIndex)
                )
                .build()
            _currentTrackName.value = trackInfo.name
        } else {
            // Mode Auto
            _player.trackSelectionParameters = _player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .build()
            updateCurrentTrackName()
        }
    }

    private fun updateVideoTracks(tracks: Tracks) {
        val newTracks = mutableListOf<VideoTrackInfo>()

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                        val trackFormat = group.getTrackFormat(i)
                        val height = trackFormat.height
                        val bitrate = trackFormat.bitrate
                        val name = if (height != -1) {
                            if (bitrate != -1) {
                                val bitrateMbps = bitrate / 1000000f
                                String.format(Locale.getDefault(), "%dp (%.1f Mbps)", height, bitrateMbps)
                            } else {
                                "${height}p"
                            }
                        } else {
                            getApplication<Application>().getString(R.string.quality_unknown)
                        }
                        newTracks.add(VideoTrackInfo(name, group.mediaTrackGroup, i, height, bitrate))
                    }
                }
            }
        }

        // Tri : D'abord par hauteur dÃ©croissante (meilleure qualitÃ© en haut), puis par bitrate dÃ©croissant
        newTracks.sortWith(compareByDescending<VideoTrackInfo> { it.height }.thenByDescending { it.bitrate })

        // Option Auto en premier (toujours au dÃ©but de la liste)
        newTracks.add(0, VideoTrackInfo("Auto", null, null))

        _videoTracks.value = newTracks
    }

    private fun updateCurrentTrackName() {
        val parameters = _player.trackSelectionParameters

        if (parameters.overrides.isEmpty()) {
            val height = lastVideoHeight
            if (height > 0) {
                _currentTrackName.value = try {
                    getApplication<Application>().getString(
                        R.string.quality_auto_with_resolution,
                        height
                    )
                } catch (_: Exception) {
                    // Fallback : utilise la string Auto simple
                    getApplication<Application>().getString(R.string.quality_auto)
                }
            } else {
                _currentTrackName.value = getApplication<Application>().getString(R.string.quality_auto)
            }
        } else {
            // Si on est en manuel, le nom est dÃ©jÃ  mis Ã  jour lors de la sÃ©lection,
            // mais on peut vÃ©rifier si l'override correspond toujours
            val override = parameters.overrides.values.firstOrNull()
            if (override != null) {
                val matching = _videoTracks.value.find {
                    it.group == override.mediaTrackGroup && override.trackIndices.contains(it.trackIndex)
                }
                _currentTrackName.value = matching?.name ?: getApplication<Application>().getString(R.string.quality_manual)
            }
        }
    }

    /**
     * Coroutine de synchronisation position/buffer (1x/s), annulÃ©e dans [onCleared].
     */
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                _bufferedPosition.value = _player.bufferedPosition.coerceAtLeast(0L)
                if (_player.isPlaying) {
                    _currentPosition.value = _player.currentPosition.coerceAtLeast(0L)
                    _duration.value = _player.duration.coerceAtLeast(0L)
                }
                delay(1000)
            }
        }
    }

    /**
     * âš ï¸ VITAL â€” LibÃ¨re IMPÃ‰RATIVEMENT le player quand le ViewModel est **dÃ©finitivement**
     * dÃ©truit (Activity `finish()`/back â€” PAS une simple rotation d'Ã©cran, qui ne dÃ©truit
     * pas le ViewModel). Sans cet appel, ExoPlayer retient des ressources natives
     * (Surface, decoders, threads de dÃ©codage) â†’ fuite mÃ©moire garantie en production
     * et rejet possible lors de l'audit Play Store (Memory Leaks / Android Vitals).
     */
    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        pendingRequestHeaders = emptyMap()
        _player.removeListener(playerListener)
        _player.release()
    }

    companion object {
        @Volatile
        private var pendingRequestHeaders: Map<String, String> = emptyMap()

        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 STV/1.0"

        private class HeaderAwareDataSourceFactory : DataSource.Factory {
            override fun createDataSource(): DataSource {
                val configuredFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(8_000)
                    .setUserAgent(USER_AGENT)

                val headers = cleanHeaders(pendingRequestHeaders)
                if (headers.isNotEmpty()) {
                    configuredFactory.setDefaultRequestProperties(headers)
                }

                return configuredFactory.createDataSource()
            }
        }

        private fun cleanHeaders(headers: Map<String, String>): Map<String, String> {
            if (headers.isEmpty()) return emptyMap()

            val cleaned = linkedMapOf<String, String>()
            for ((rawKey, rawValue) in headers) {
                val key = rawKey.trim().takeIf { it.isNotEmpty() } ?: continue
                val value = rawValue.trim().replace("\r", " ").replace("\n", " ").takeIf { it.isNotEmpty() }
                    ?: continue
                cleaned[key] = value
            }
            return cleaned
        }

        /**
         * Factory par dÃ©faut de l'ExoPlayer.
         *
         * âœ… SÃ‰CURITÃ‰ MÃ‰MOIRE : reÃ§oit explicitement un [Application] (JAMAIS une Activity)
         * â†’ aucune rÃ©fÃ©rence Ã  un `Context` d'Activity n'est jamais retenue par le player,
         * ce qui Ã©limine tout risque de fuite mÃ©moire mÃªme si l'Activity est dÃ©truite
         * pendant que le ViewModel (et donc le player) continue de vivre.
         */
        @androidx.annotation.OptIn(UnstableApi::class)
        fun createDefaultExoPlayer(application: Application): ExoPlayer {
            val trackSelector = DefaultTrackSelector(application)

            val mediaSourceFactory = DefaultMediaSourceFactory(HeaderAwareDataSourceFactory())

            val renderersFactory = DefaultRenderersFactory(application)
                .setEnableDecoderFallback(true)

            // Optimisation du Buffer pour un dÃ©marrage ultra-rapide et une reprise plus tolÃ©rante
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000, // minBufferMs
                    50_000, // maxBufferMs
                    800,    // bufferForPlaybackMs
                    1_500   // bufferForPlaybackAfterRebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            return ExoPlayer.Builder(application)
                .setRenderersFactory(renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true) // Activer gestion focus audio
                .setHandleAudioBecomingNoisy(true) // Pause sur dÃ©connexion Ã©couteurs
                .build()
        }
    }
}

