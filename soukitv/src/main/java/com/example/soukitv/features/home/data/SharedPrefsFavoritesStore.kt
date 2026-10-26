package com.example.soukitv.features.home.data

import android.content.Context
import com.example.soukitv.features.home.domain.repository.FavoritesStore

class SharedPrefsFavoritesStore(context: Context) : FavoritesStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("souki_favorites", Context.MODE_PRIVATE)

    override fun load(): Set<String> =
        prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()

    override fun save(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_IDS, ids).apply()
    }

    private companion object {
        const val KEY_IDS = "favorite_channel_ids"
    }
}

