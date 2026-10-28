package com.example.soukitv.features.home.data.remote

import com.example.soukitv.features.home.data.model.ChannelDto
import com.example.soukitv.features.home.data.model.MatchItemDto
import retrofit2.http.GET
import retrofit2.http.Url

/** API distante : ne manipule que des DTOs ; le mapping vers le Domain est fait par le repository. */
interface ChannelApiService {
    @GET("")
    suspend fun getChannels(@Url fullUrl: String): List<ChannelDto>

    @GET("")
    suspend fun getMatches(@Url fullUrl: String): List<MatchItemDto>
}

