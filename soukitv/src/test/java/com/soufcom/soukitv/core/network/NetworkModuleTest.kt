package com.soufcom.soukitv.core.network

import com.soufcom.soukitv.core.security.NetworkSecurityProvider
import okhttp3.CertificatePinner
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `default client has no active certificate pins`() {
        assertTrue(NetworkModule.createOkHttpClient().certificatePinner.pins.isEmpty())
    }

    @Test
    fun `injected certificate pinner is applied to the shared client`() {
        val pinner = CertificatePinner.Builder()
            .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        val client = NetworkModule.createOkHttpClient(object : NetworkSecurityProvider {
            override fun certificatePinner(): CertificatePinner = pinner
        })

        assertTrue(client.certificatePinner.pins.containsAll(pinner.pins))
    }
}
