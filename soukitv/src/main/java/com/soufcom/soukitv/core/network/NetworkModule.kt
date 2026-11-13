package com.soufcom.soukitv.core.network

import com.soufcom.soukitv.core.security.DisabledNetworkSecurityProvider
import com.soufcom.soukitv.core.security.NetworkSecurityProvider
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Point unique de construction du client HTTP Retrofit de :soukitv. */
object NetworkModule {
    fun createOkHttpClient(
        securityProvider: NetworkSecurityProvider = DisabledNetworkSecurityProvider
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                securityProvider.certificatePinner()?.let(::certificatePinner)
            }
            .build()
    }
}

