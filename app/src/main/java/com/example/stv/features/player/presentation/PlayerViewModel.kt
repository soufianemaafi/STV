package com.example.stv.features.player.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.stv.R
import com.example.stv.core.domain.model.VideoTrackInfo
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel MVI du player vidéo — **seul et unique propriétaire** du cycle de vie d'ExoPlayer.
 *
 * ## 🔒 Sécurité mémoire (Mission 2 — isolation Media3 dans la couche Presentation)
 * - Le [ExoPlayer] est construit **une seule fois**, avec le contexte de l'[Application]
 *   (jamais celui d'une Activity) via [playerFactory]/[createDefaultExoPlayer] → aucune
 *   fuite mémoire possible liée à la rétention d'une référence Activity après sa destruction.
 * - Ce ViewModel (`AndroidViewModel`) **survit aux rotations d'écran** (l'Activity est
 *   recréée, pas le ViewModel) : le player n'est donc JAMAIS recréé lors d'une rotation,
 *   la lecture continue sans interruption.
 * - [onCleared] appelle **IMPÉRATIVEMENT** `player.release()` quand le ViewModel est
 *   définitivement détruit (Activity `finish()`/back) — zéro fuite mémoire garantie.
 * - L'Activity/UI (`PlayerActivity` / composable `VideoPlayer`) ne fait qu'**attacher /
 *   détacher** une `PlayerView` à [player] (`playerView.player = viewModel.player` puis
 *   `= null`) — elle ne possède ni ne contrôle jamais l'instance du player directement ;
 *   tout contrôle passe par [onAction] (MVI).
 *
 * @param playerFactory Factory de construction du player, injectable pour les tests
 * unitaires (permet d'injecter un [Player] mocké — interface pure — plutôt qu'un
 * [ExoPlayer] concret qui nécessite un environnement Android réel). Par défaut :
 * [createDefaultExoPlayer], qui utilise TOUJOURS le contexte Application.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel @JvmOverloads constructor(
    application: Application,
    private val playerFactory: (Application) -> Player = ::createDefaultExoPlayer
) : AndroidViewModel(application) {

    // ✅ SÉCURITÉ MÉMOIRE : construit UNE SEULE FOIS avec le contexte Application.
    // Ne JAMAIS reconstruire avec un contexte d'Activity (fuite mémoire garantie).
    // Typé [Player] (interface Media3) — seules les capacités de contrôle de lecture
    // communes sont utilisées ici, ce qui permet aussi de tester ce ViewModel avec un
    // Player mocké sans dépendre de la classe concrète ExoPlayer (native/Android).
    private val _player: Player = playerFactory(application)

    /**
     * Instance exposée à l'UI. L'UI ne doit s'en servir QUE pour l'attacher/détacher
     * d'une `PlayerView` (`playerView.player = viewModel.player`) — tout contrôle
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

    // ✅ Détection flux LIVE (HLS live, DASH live, etc.)
    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    // State machine : null = pas encore initialisé (attend une PlayerUiAction)
    private val _uiState = MutableStateFlow<PlayerUiState?>(null)
    val uiState: StateFlow<PlayerUiState?> = _uiState.asStateFlow()

    private var currentUrl: String? = null

    // ✅ Dernière hauteur vidéo connue (via onVideoSizeChanged), utilisée pour afficher
    // "Auto (1080p)" sans dépendre de `ExoPlayer.videoFormat` (spécifique à la classe
    // concrète) — garde `_player` typé [Player] uniquement (interface pure, testable).
    private var lastVideoHeight: Int = 0

    // ✅ Job explicite pour annuler la coroutine de position dans onCleared (pas de fuite de coroutine)
    private var positionUpdateJob: Job? = null

    /**
     * Synchronise le `StateFlow<PlayerUiState>` (et les autres StateFlows) avec les
     * événements réels du player Media3. Ajouté une seule fois, à la création du ViewModel.
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
     * Point d'entrée MVI **unique** pour la View. Aucune logique métier côté
     * Activity/Composable : tout contrôle du player passe par ici.
     *
     * Appelé depuis `PlayerActivity` (`onCreate`/`onNewIntent`) et depuis le
     * composable `VideoPlayer` (contrôles play/pause/seek/qualité).
     */
    fun onAction(action: PlayerUiAction) {
        when (action) {
            is PlayerUiAction.LoadVideo -> {
                currentUrl = action.videoUrl
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
     * Met à jour l'état UI directement. Utilisé par `PlayerActivity` pour les
     * transitions liées au flux publicitaire (ads dismissed/failed/network error),
     * qui restent orchestrées côté Activity via `AdsController` (hors scope Media3).
     */
    fun setUiState(newState: PlayerUiState) {
        _uiState.value = newState
    }

    /** Vérifie si l'état actuel est Ready */
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
     * Construit le [MediaItem] et lance la préparation du player : `setMediaItem` + `prepare()`.
     * N'est appelé qu'une seule fois par URL (idempotent) — une rotation d'écran ne
     * relance donc jamais la préparation puisque le ViewModel (et `currentUrl`) survit.
     */
    private fun preparePlayback(url: String) {
        if (currentUrl == url && _player.playbackState != Player.STATE_IDLE) return

        currentUrl = url
        _isLoading.value = true
        _errorMessage.value = null

        _player.playWhenReady = true
        _player.setMediaItem(MediaItem.fromUri(url))
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
            // Sélection manuelle
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

        // Tri : D'abord par hauteur décroissante (meilleure qualité en haut), puis par bitrate décroissant
        newTracks.sortWith(compareByDescending<VideoTrackInfo> { it.height }.thenByDescending { it.bitrate })

        // Option Auto en premier (toujours au début de la liste)
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
            // Si on est en manuel, le nom est déjà mis à jour lors de la sélection,
            // mais on peut vérifier si l'override correspond toujours
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
     * Coroutine de synchronisation position/buffer (1x/s), annulée dans [onCleared].
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
     * ⚠️ VITAL — Libère IMPÉRATIVEMENT le player quand le ViewModel est **définitivement**
     * détruit (Activity `finish()`/back — PAS une simple rotation d'écran, qui ne détruit
     * pas le ViewModel). Sans cet appel, ExoPlayer retient des ressources natives
     * (Surface, decoders, threads de décodage) → fuite mémoire garantie en production
     * et rejet possible lors de l'audit Play Store (Memory Leaks / Android Vitals).
     */
    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        _player.removeListener(playerListener)
        _player.release()
    }

    companion object {
        /**
         * Factory par défaut de l'ExoPlayer.
         *
         * ✅ SÉCURITÉ MÉMOIRE : reçoit explicitement un [Application] (JAMAIS une Activity)
         * → aucune référence à un `Context` d'Activity n'est jamais retenue par le player,
         * ce qui élimine tout risque de fuite mémoire même si l'Activity est détruite
         * pendant que le ViewModel (et donc le player) continue de vivre.
         */
        @androidx.annotation.OptIn(UnstableApi::class)
        fun createDefaultExoPlayer(application: Application): ExoPlayer {
            val trackSelector = DefaultTrackSelector(application)

            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8_000)
                .setReadTimeoutMs(8_000)
                .setUserAgent(
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 STV/1.0"
                )

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(application)
                .setEnableDecoderFallback(true)

            // Optimisation du Buffer pour un démarrage ultra-rapide et une reprise plus tolérante
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
                .setHandleAudioBecomingNoisy(true) // Pause sur déconnexion écouteurs
                .build()
        }
    }
}
