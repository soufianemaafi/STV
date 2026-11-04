package com.example.soukitv.features.home.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.RemoteConfig

interface ChannelRepository {
    fun getCategories(): Flow<List<Category>>
    fun getMatches(): Flow<List<MatchItem>>
    fun getRemoteConfig(): Flow<RemoteConfig>
}


