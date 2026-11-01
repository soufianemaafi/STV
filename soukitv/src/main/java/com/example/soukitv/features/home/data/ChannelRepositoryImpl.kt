package com.example.soukitv.features.home.data

import android.util.Log
import com.example.soukitv.features.home.data.mapper.toCategories
import com.example.soukitv.features.home.data.mapper.toDomain
import com.example.soukitv.features.home.data.model.RemoteCatalogConfigDto
import com.example.soukitv.features.home.data.remote.ChannelApiService
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChannelRepositoryImpl : ChannelRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val apiService: ChannelApiService = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        .create(ChannelApiService::class.java)

    override fun getCategories(): Flow<List<Category>> = flow {
        emit(
            withContext(Dispatchers.IO) {
                apiService.getChannels(REMOTE_CHANNELS_URL)
            }.toDomain().toCategories()
        )
    }

    override fun getMinStvVersionCode(): Flow<Long> = flow {
        val minVersion = try {
            withContext(Dispatchers.IO) {
                apiService.getRemoteConfig(REMOTE_CONFIG_URL)
            }.minStvVersionCode
                .takeIf { it > 0L }
                ?: RemoteCatalogConfigDto.DEFAULT_MIN_STV_VERSION_CODE
        } catch (throwable: Exception) {
            Log.w(TAG, "Remote config unavailable, using default minStvVersionCode", throwable)
            RemoteCatalogConfigDto.DEFAULT_MIN_STV_VERSION_CODE
        }

        emit(minVersion)
    }

    /** Matchs du jour récupérés depuis [REMOTE_MATCHES_URL] sur Dispatchers.IO. */
    override fun getMatches(): Flow<List<MatchItem>> = flow {
        emit(
            withContext(Dispatchers.IO) {
                apiService.getMatches(REMOTE_MATCHES_URL)
            }.toDomain()
        )
    }

    companion object {
        private const val TAG = "ChannelRepositoryImpl"
        private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/config.json"
        private const val REMOTE_CHANNELS_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/channels.json"
        private const val REMOTE_MATCHES_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/matches.json"
    }
}
