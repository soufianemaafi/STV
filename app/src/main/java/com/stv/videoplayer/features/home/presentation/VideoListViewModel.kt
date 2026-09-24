package com.stv.videoplayer.features.home.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stv.videoplayer.core.domain.model.VideoItem
import com.stv.videoplayer.data.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ViewModel pour la gestion de la liste des vidÃ©os.
 * DÃ©lÃ¨gue la persistance Ã  VideoRepository (prÃªt pour migration Room).
 */
class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val stateMutex = Mutex()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    init {
        refreshVideos()
    }

    fun addVideo(item: VideoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            stateMutex.withLock {
                val updated = _videos.value.toMutableList().apply { add(0, item) }
                _videos.value = updated
                repository.saveVideos(updated)
            }
        }
    }

    fun removeVideo(item: VideoItem) {
        if (repository.isDefaultVideo(item)) return
        viewModelScope.launch(Dispatchers.IO) {
            stateMutex.withLock {
                val updated = _videos.value.filterNot { it.id == item.id }
                _videos.value = updated
                repository.saveVideos(updated)
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            stateMutex.withLock {
                _videos.value = repository.loadVideos()
            }
        }
    }

    fun isDefaultVideo(item: VideoItem): Boolean {
        return repository.isDefaultVideo(item)
    }
}

