package com.soufcom.soukitv.core.security

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAppIntegrityGuardTest {
    // Les contrôles stricts sont désactivés en BuildConfig.DEBUG (variante testée en JVM).
    @Test
    fun isTrusted_returnsTrue_inDebugBuilds() {
        assertTrue(DefaultAppIntegrityGuard(mockk<Context>(relaxed = true)).isTrusted())
    }
}
