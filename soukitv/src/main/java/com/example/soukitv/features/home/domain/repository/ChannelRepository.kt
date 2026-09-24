package com.example.soukitv.features.home.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.soukitv.features.home.domain.model.Category

interface ChannelRepository {
    fun getCategories(): Flow<List<Category>>
}


