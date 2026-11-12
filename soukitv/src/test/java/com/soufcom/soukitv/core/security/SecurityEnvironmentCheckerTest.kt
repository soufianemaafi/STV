package com.soufcom.soukitv.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityEnvironmentCheckerTest {
    @Test
    fun isFridaPortLine_detectsListeningPort27042() {
        val procLine = "  42: 0100007F:69A2 00000000:0000 0A 00000000:0000 00:00000000 00000000"

        assertTrue(SecurityEnvironmentChecker.isFridaPortLine(procLine))
    }

    @Test
    fun isFridaPortLine_rejectsOtherPortsAndStates() {
        val wrongPort = "  42: 0100007F:69A3 00000000:0000 0A 00000000:0000"
        val wrongState = "  42: 0100007F:69A2 00000000:0000 01 00000000:0000"

        assertFalse(SecurityEnvironmentChecker.isFridaPortLine(wrongPort))
        assertFalse(SecurityEnvironmentChecker.isFridaPortLine(wrongState))
    }

    @Test
    fun containsSuspiciousLibrary_detectsFridaAndXposedMaps() {
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/data/local/tmp/frida-agent.so"))
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/system/lib64/libxposed.so"))
        assertFalse(SecurityEnvironmentChecker.containsSuspiciousLibrary("/system/lib64/libandroid.so"))
    }
}

