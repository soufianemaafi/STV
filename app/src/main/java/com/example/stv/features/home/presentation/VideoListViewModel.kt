package com.example.stv.features.home.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.stv.core.domain.model.VideoItem
import com.example.stv.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel pour la gestion de la liste des vidéos.
 * Délègue la persistance à VideoRepository (prêt pour migration Room).
 */
class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)

    private val _videos = MutableStateFlow(repository.loadVideos())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    fun addVideo(item: VideoItem) {
        val updated = _videos.value.toMutableList().apply { add(0, item) }
        _videos.value = updated
        repository.saveVideos(updated)
    }

    fun removeVideo(item: VideoItem) {
        if (repository.isDefaultVideo(item)) return
        val updated = _videos.value.filterNot { it.id == item.id }
        _videos.value = updated
        repository.saveVideos(updated)
    }

    fun refreshVideos() {
        _videos.value = repository.loadVideos()
    }

    fun isDefaultVideo(item: VideoItem): Boolean {
        return repository.isDefaultVideo(item)
    }
}
