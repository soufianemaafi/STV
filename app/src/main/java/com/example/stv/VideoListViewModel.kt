package com.example.stv

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ✅ Kotlin Serialization : instance configurée pour tolérance
    private val json = Json {
        ignoreUnknownKeys = true   // Ignore les clés inconnues (compatibilité future)
        encodeDefaults = true      // Encode les valeurs par défaut (id UUID)
    }

    private val _videos = MutableStateFlow(loadVideos())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    fun addVideo(item: VideoItem) {
        val updated = _videos.value.toMutableList().apply { add(0, item) }
        _videos.value = updated
        saveVideos(updated)
    }

    fun removeVideo(item: VideoItem) {
        if (isDefaultVideo(item)) return
        // ✅ Filtrer par ID unique (et non par titre+url) pour éviter de supprimer les doublons
        val updated = _videos.value.filterNot { it.id == item.id }
        _videos.value = updated
        saveVideos(updated)
    }

    fun refreshVideos() {
        _videos.value = loadVideos()
    }

    fun isDefaultVideo(item: VideoItem): Boolean {
        return item.url == DEFAULT_VIDEO_URL
    }

    // ✅ Kotlin Serialization : décodage typé automatique (remplace JSONArray/JSONObject manuel)
    private fun loadVideos(): List<VideoItem> {
        val stored = prefs.getString(PREFS_KEY, null) ?: return defaultVideos()
        return try {
            val list = json.decodeFromString<List<VideoItem>>(stored)
            if (list.isEmpty()) defaultVideos() else list
        } catch (e: Exception) {
            Log.w("VideoListViewModel", "Error loading videos, resetting to defaults", e)
            defaultVideos()
        }
    }

    // ✅ Kotlin Serialization : encodage typé automatique (remplace JSONArray/JSONObject manuel)
    private fun saveVideos(videos: List<VideoItem>) {
        val encoded = json.encodeToString(videos)
        prefs.edit {
            putString(PREFS_KEY, encoded)
        }
    }

    private fun defaultVideos(): List<VideoItem> {
        return listOf(
            VideoItem(
                title = "Big Buck Bunny",
                url = DEFAULT_VIDEO_URL
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "stv_videos"
        private const val PREFS_KEY = "videos_json"
        private const val DEFAULT_VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}
