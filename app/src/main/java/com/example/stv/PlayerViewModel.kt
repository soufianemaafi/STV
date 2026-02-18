package com.example.stv


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

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

    private val _currentTrackName = MutableStateFlow(context.getString(R.string.quality_auto))
    val currentTrackName: StateFlow<String> = _currentTrackName.asStateFlow()

    private var trackSelector: DefaultTrackSelector? = null
    private var currentUrl: String? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _isLoading.value = true
                Player.STATE_READY -> {
                    _isLoading.value = false
                    _duration.value = _exoPlayer?.duration ?: 0L
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
            _errorMessage.value = "Erreur de lecture: ${error.message}"
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateVideoTracks(tracks)
            updateCurrentTrackName()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            updateCurrentTrackName()
        }
    }

    fun initializePlayer(url: String) {
        if (_exoPlayer != null && currentUrl == url) return

        releasePlayer()
        currentUrl = url
        _isLoading.value = true
        _errorMessage.value = null

        trackSelector = DefaultTrackSelector(context)

        _exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector!!)
            .build()
            .apply {
                addListener(playerListener)
                playWhenReady = true
                setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                prepare()
            }

        // Lancer une coroutine pour mettre à jour la position régulièrement
        viewModelScope.launch {
            while (true) {
                val player = _exoPlayer
                if (player != null && player.isPlaying) {
                    _currentPosition.value = player.currentPosition
                    _bufferedPosition.value = player.bufferedPosition
                    _duration.value = player.duration.coerceAtLeast(0L)
                } else if (player != null) {
                    // Update buffer even when paused
                    _bufferedPosition.value = player.bufferedPosition
                }
                delay(1000) // Mise à jour chaque seconde
            }
        }
    }

    fun togglePlayPause() {
        val player = _exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekForward() {
        _exoPlayer?.let { player ->
            seekTo(player.currentPosition + 10000)
        }
    }

    fun seekRewind() {
        _exoPlayer?.let { player ->
            seekTo(player.currentPosition - 10000)
        }
    }

    fun selectTrack(trackInfo: VideoTrackInfo) {
        val player = _exoPlayer ?: return
        // val selector = trackSelector ?: return // selector inutilisé

        if (trackInfo.group != null && trackInfo.trackIndex != null) {
            // Sélection manuelle
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(trackInfo.group, trackInfo.trackIndex)
                )
                .build()
            _currentTrackName.value = trackInfo.name
        } else {
            // Mode Auto
            player.trackSelectionParameters = player.trackSelectionParameters
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
                                String.format("%dp (%.1f Mbps)", height, bitrateMbps)
                            } else {
                                "${height}p"
                            }
                        } else {
                            "Inconnu"
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
        val player = _exoPlayer ?: return
        val parameters = player.trackSelectionParameters

        if (parameters.overrides.isEmpty()) {
            // val width = player.videoFormat?.width ?: 0 // width inutilisé
            val height = player.videoFormat?.height ?: 0
            _currentTrackName.value = if (height > 0) "Auto (${height}p)" else "Auto"
        } else {
             // Si on est en manuel, le nom est déjà mis à jour lors de la sélection,
             // mais on peut vérifier si l'override correspond toujours
             val override = parameters.overrides.values.firstOrNull()
             if (override != null) {
                 val matching = _videoTracks.value.find {
                     it.group == override.mediaTrackGroup && override.trackIndices.contains(it.trackIndex)
                 }
                 _currentTrackName.value = matching?.name ?: "Manuel"
             }
        }
    }

    fun releasePlayer() {
        _exoPlayer?.let { player ->
            player.removeListener(playerListener)
            player.release()
        }
        _exoPlayer = null
        trackSelector = null
        currentUrl = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
