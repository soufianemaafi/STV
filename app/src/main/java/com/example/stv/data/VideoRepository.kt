package com.example.stv.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.stv.VideoItem
import kotlinx.serialization.json.Json

/**
 * Repository pour la persistance des vidéos.
 *
 * Implémentation actuelle : SharedPreferences + Kotlin Serialization (JSON).
 * Suffisant pour une liste courte (<100 items).
 *
 * Pour une migration future vers Room :
 * 1. Créer une implémentation RoomVideoRepository
 * 2. Remplacer l'injection dans VideoListViewModel
 * 3. Le ViewModel ne change pas (dépend uniquement de l'interface)
 */
class VideoRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadVideos(): List<VideoItem> {
        val stored = prefs.getString(PREFS_KEY, null) ?: return defaultVideos()
        return try {
            val list = json.decodeFromString<List<VideoItem>>(stored)
            if (list.isEmpty()) defaultVideos() else list
        } catch (e: Exception) {
            Log.w(TAG, "Error loading videos, resetting to defaults", e)
            defaultVideos()
        }
    }

    fun saveVideos(videos: List<VideoItem>) {
        val encoded = json.encodeToString(videos)
        prefs.edit {
            putString(PREFS_KEY, encoded)
        }
    }

    fun isDefaultVideo(item: VideoItem): Boolean {
        return item.url == DEFAULT_VIDEO_URL
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
        private const val TAG = "VideoRepository"
        private const val PREFS_NAME = "stv_videos"
        private const val PREFS_KEY = "videos_json"
        private const val DEFAULT_VIDEO_URL =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}

