package com.soufcom.soukitv.features.home.data

import com.soufcom.soukitv.core.network.NetworkModule
import com.soufcom.soukitv.core.security.AppIntegrityGuard
import com.soufcom.soukitv.core.security.SecurityVault
import com.soufcom.soukitv.core.security.UnavailableSecurityVault
import com.soufcom.soukitv.core.security.defaultTrustedHosts
import com.soufcom.soukitv.features.home.data.mapper.toCategories
import com.soufcom.soukitv.features.home.data.mapper.toDomain
import com.soufcom.soukitv.features.home.data.model.RemoteCatalogConfigDto
import com.soufcom.soukitv.features.home.data.remote.ChannelApiService
import com.soufcom.soukitv.features.home.domain.model.Category
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.RemoteConfig
import com.soufcom.soukitv.features.home.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Hôtes nettoyés du JSON ; repli obfusqué si absent ou vide. */
internal fun RemoteCatalogConfigDto.toTrustedHosts(
    securityVault: SecurityVault
): Set<String> =
    trustedHosts.orEmpty()
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && it != "*" }
        .toSet()
        .ifEmpty { defaultTrustedHosts(securityVault) }

class ChannelRepositoryImpl(
    private val integrityGuard: AppIntegrityGuard? = null,
    private val securityVault: SecurityVault = UnavailableSecurityVault
) : ChannelRepository {

    private val httpClient = NetworkModule.createOkHttpClient()

    override fun getCategories(): Flow<List<Category>> = flow {
        ensureTrusted()
        emit(
            withContext(Dispatchers.IO) {
                apiService().getChannels(
                    requireRemoteUrl(REMOTE_CHANNELS_KEY),
                    timestamp = System.currentTimeMillis()
                )
            }.toCategories()
        )
    }

    override fun getRemoteConfig(): Flow<RemoteConfig> = flow {
        ensureTrusted()
        val dto = try {
            withContext(Dispatchers.IO) {
                apiService().getRemoteConfig(
                    requireRemoteUrl(REMOTE_CONFIG_KEY),
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (throwable: Exception) {
            if (throwable is CancellationException) throw throwable
            throw SecurityException("Remote security configuration unavailable", throwable)
        }

        val authSecret = dto.stvAuthSecret
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw SecurityException("Remote security configuration unavailable")

        emit(
            RemoteConfig(
                minVersionCode = dto.minStvVersionCode
                    .takeIf { it > 0L }
                    ?: RemoteCatalogConfigDto.DEFAULT_MIN_STV_VERSION_CODE,
                authSecret = authSecret,
                trustedHosts = dto.toTrustedHosts(securityVault)
            )
        )
    }


    /** Matchs du jour récupérés depuis [REMOTE_MATCHES_URL] sur Dispatchers.IO. */
    override fun getMatches(): Flow<List<MatchItem>> = flow {
        ensureTrusted()
        emit(
            withContext(Dispatchers.IO) {
                apiService().getMatches(
                    requireRemoteUrl(REMOTE_MATCHES_KEY),
                    timestamp = System.currentTimeMillis()
                )
            }.toDomain()
        )
    }

    private suspend fun ensureTrusted() {
        val guard = integrityGuard ?: return
        if (!withContext(Dispatchers.IO) { guard.isTrusted() }) {
            throw SecurityException("Application integrity check failed")
        }
    }

    private fun apiService(): ChannelApiService =
        Retrofit.Builder()
            .baseUrl(requireRemoteUrl(REMOTE_BASE_KEY))
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ChannelApiService::class.java)

    private fun requireRemoteUrl(key: Int): String =
        securityVault.remoteUrl(key)
            .trim()
            .takeIf(String::isNotEmpty)
            ?: throw SecurityException("Remote security configuration unavailable")

    companion object {
        private const val REMOTE_BASE_KEY = 0
        private const val REMOTE_CONFIG_KEY = 1
        private const val REMOTE_CHANNELS_KEY = 2
        private const val REMOTE_MATCHES_KEY = 3
    }
}
