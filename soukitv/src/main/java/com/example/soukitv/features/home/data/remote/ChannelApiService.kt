package com.example.soukitv.features.home.data.remote

import com.example.soukitv.features.home.domain.model.Channel
import retrofit2.http.GET
import retrofit2.http.Url

interface ChannelApiService {
    @GET("")
    suspend fun getChannels(@Url fullUrl: String): List<Channel>
}

