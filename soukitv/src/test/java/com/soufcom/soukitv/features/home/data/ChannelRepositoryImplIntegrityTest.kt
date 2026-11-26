package com.soufcom.soukitv.features.home.data

import com.soufcom.soukitv.core.security.AppIntegrityGuard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChannelRepositoryImplIntegrityTest {
    private val untrusted = ChannelRepositoryImpl(AppIntegrityGuard { false })

    @Test
    fun getCategories_throwsSecurityException_whenGuardIsUntrusted() = runTest {
        assertRejected(untrusted.getCategories())
    }

    @Test
    fun getRemoteConfig_throwsSecurityException_whenGuardIsUntrusted() = runTest {
        assertRejected(untrusted.getRemoteConfig())
    }

    @Test
    fun getMatches_throwsSecurityException_whenGuardIsUntrusted() = runTest {
        assertRejected(untrusted.getMatches())
    }

    private suspend fun assertRejected(flow: Flow<*>) {
        try {
            flow.first()
            fail("SecurityException attendue")
        } catch (e: SecurityException) {
            assertTrue(e.message.orEmpty().contains("integrity"))
        }
    }
}
