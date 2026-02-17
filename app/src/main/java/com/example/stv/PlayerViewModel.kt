package com.example.stv


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

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
                Player.STATE_READY -> _isLoading.value = false
                Player.STATE_ENDED -> _isLoading.value = false
                Player.STATE_IDLE -> _isLoading.value = false
            }
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

        // Option Auto en premier
        newTracks.add(VideoTrackInfo("Auto", null, null))

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                        val trackFormat = group.getTrackFormat(i)
                        val height = trackFormat.height
                        val bitrate = trackFormat.bitrate
                        val name = if (height != -1) "${height}p" else "Inconnu"
                        newTracks.add(VideoTrackInfo(name, group.mediaTrackGroup, i, height, bitrate))
                    }
                }
            }
        }
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

