package com.soufcom.soukitv.core.security

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityVaultFailClosedTest {
    @Test
    fun unavailableVault_returnsNoSecrets() {
        assertFalse(UnavailableSecurityVault.nativeVerifyIntegrity())
        assertTrue(UnavailableSecurityVault.remoteUrl(0).isEmpty())
        assertTrue(UnavailableSecurityVault.trustedHosts().isEmpty())
        assertTrue(UnavailableSecurityVault.playerDigests().isEmpty())
    }

    @Test
    fun nativeVault_returnsNoSecretsWhenLibraryIsUnavailable() {
        val vault = NativeSecurityVault(mockk<Context>(relaxed = true))

        assertFalse(vault.nativeVerifyIntegrity())
        assertTrue(vault.remoteUrl(0).isEmpty())
        assertTrue(vault.trustedHosts().isEmpty())
        assertTrue(vault.playerDigests().isEmpty())
    }
}
