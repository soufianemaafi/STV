package com.example.stv

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _videos = MutableStateFlow(loadVideos())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    fun addVideo(item: VideoItem) {
        val updated = _videos.value.toMutableList().apply { add(0, item) }
        _videos.value = updated
        saveVideos(updated)
    }

    fun removeVideo(item: VideoItem) {
        if (isDefaultVideo(item)) return
        val updated = _videos.value.filterNot { it.title == item.title && it.url == item.url }
        _videos.value = updated
        saveVideos(updated)
    }

    fun isDefaultVideo(item: VideoItem): Boolean {
        return item.url == DEFAULT_VIDEO_URL
    }

    private fun loadVideos(): List<VideoItem> {
        val stored = prefs.getString(PREFS_KEY, null) ?: return defaultVideos()
        return try {
            val array = JSONArray(stored)
            val list = mutableListOf<VideoItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(VideoItem(obj.getString("title"), obj.getString("url")))
            }
            if (list.isEmpty()) defaultVideos() else list
        } catch (e: Exception) {
            defaultVideos()
        }
    }

    private fun saveVideos(videos: List<VideoItem>) {
        val array = JSONArray()
        videos.forEach { item ->
            val obj = JSONObject().apply {
                put("title", item.title)
                put("url", item.url)
            }
            array.put(obj)
        }
        prefs.edit().putString(PREFS_KEY, array.toString()).apply()
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
