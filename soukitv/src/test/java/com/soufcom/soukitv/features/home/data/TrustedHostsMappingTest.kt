package com.soufcom.soukitv.features.home.data

import com.google.gson.Gson
import com.soufcom.soukitv.core.security.TestSecurityVault
import com.soufcom.soukitv.core.security.defaultTrustedHosts
import com.soufcom.soukitv.features.home.data.model.RemoteCatalogConfigDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrustedHostsMappingTest {

    private val gson = Gson()

    @Test
    fun trustedHostsPresent_areCleaned() {
        val dto = gson.fromJson(
            """{"stvAuthSecret":"s","trustedHosts":[" Edge.Example.com ","","*","  "]}""",
            RemoteCatalogConfigDto::class.java
        )

        assertEquals(setOf("edge.example.com"), dto.toTrustedHosts(TestSecurityVault))
    }

    @Test
    fun trustedHostsAbsent_usesObfuscatedFallback() {
        val dto = gson.fromJson("""{"stvAuthSecret":"s"}""", RemoteCatalogConfigDto::class.java)

        assertNull(dto.trustedHosts)
        assertEquals(
            defaultTrustedHosts(TestSecurityVault),
            dto.toTrustedHosts(TestSecurityVault)
        )
    }

    @Test
    fun trustedHostsEmptyOrOnlyWildcard_usesFallback() {
        assertEquals(
            defaultTrustedHosts(TestSecurityVault),
            RemoteCatalogConfigDto(trustedHosts = listOf("*", " "))
                .toTrustedHosts(TestSecurityVault)
        )
    }
}
