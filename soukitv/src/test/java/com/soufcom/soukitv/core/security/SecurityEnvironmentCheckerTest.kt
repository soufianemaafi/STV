package com.soufcom.soukitv.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityEnvironmentCheckerTest {
    private fun contextWithPackages(installed: Set<String>): Context {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        val filesDir = mockk<File>()
        every { filesDir.absolutePath } returns "/data/user/0/com.soufcom.soukitv/files"
        every { context.filesDir } returns filesDir
        every { context.packageName } returns "com.soufcom.soukitv"
        every { context.applicationInfo } returns ApplicationInfo()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns null
        every { context.packageManager } returns packageManager
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } answers {
            if (firstArg<String>() !in installed) throw PackageManager.NameNotFoundException()
            PackageInfo()
        }
        return context
    }

    @Test
    fun assessEnvironment_healthyDeviceIsSafe() {
        assertEquals(
            SecurityAssessment.SAFE,
            SecurityEnvironmentChecker.assessEnvironment(contextWithPackages(emptySet()))
        )
    }

    @Test
    fun assessEnvironment_networkToolsAloneBlockWithGenericScreen() {
        listOf("com.emanuelef.remote_capture", "com.reqable.android", "com.network.proxy").forEach {
            assertEquals(
                SecurityAssessment.NETWORK_ENVIRONMENT_BLOCKED,
                SecurityEnvironmentChecker.assessEnvironment(contextWithPackages(setOf(it)))
            )
        }
    }

    @Test
    fun assessEnvironment_systemProxyAloneBlocksWithGenericScreen() {
        System.setProperty("http.proxyHost", "127.0.0.1")
        try {
            assertEquals(
                SecurityAssessment.NETWORK_ENVIRONMENT_BLOCKED,
                SecurityEnvironmentChecker.assessEnvironment(contextWithPackages(emptySet()))
            )
        } finally {
            System.clearProperty("http.proxyHost")
        }
    }

    @Test
    fun assessEnvironment_tamperToolsCloseSilently() {
        listOf("bin.mt.plus", "com.chelpus.lackypatch", "com.topjohnwu.magisk").forEach {
            assertEquals(
                SecurityAssessment.COMPROMISED_SILENT_EXIT,
                SecurityEnvironmentChecker.assessEnvironment(contextWithPackages(setOf(it)))
            )
        }
    }

    @Test
    fun assessEnvironment_severeThreatTakesPriorityOverNetworkTool() {
        val context = contextWithPackages(setOf("bin.mt.plus", "com.emanuelef.remote_capture"))
        assertEquals(
            SecurityAssessment.COMPROMISED_SILENT_EXIT,
            SecurityEnvironmentChecker.assessEnvironment(context)
        )
    }

    @Test
    fun isEnvironmentSafe_falseForBothUnsafeCases() {
        assertFalse(
            SecurityEnvironmentChecker.isEnvironmentSafe(
                contextWithPackages(setOf("com.reqable.android"))
            )
        )
        assertFalse(
            SecurityEnvironmentChecker.isEnvironmentSafe(
                contextWithPackages(setOf("bin.mt.plus"))
            )
        )
    }

    @Test
    fun hasHostilePackage_detectsNetworkCaptureAndApkModificationTools() {
        val newHostilePackages = setOf(
            "com.reqable.android",
            "com.network.proxy",
            "tech.httptoolkit.android.v1",
            "jp.co.taosoftware.android.packetcapture",
            "bin.mt.plus",
            "bin.mt.plus.canary"
        )
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } answers {
            if (firstArg<String>() !in newHostilePackages) {
                throw PackageManager.NameNotFoundException()
            }
            PackageInfo()
        }

        assertTrue(SecurityEnvironmentChecker.hasHostilePackage(context))
    }

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

    @Test
    fun containsSuspiciousLibrary_detectsLsposedZygiskAndMagisk() {
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/data/adb/modules/zygisk_lsposed/liblspd.so"))
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/dev/__properties__/magisk"))
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/system/lib64/libriru_edxp.so"))
        assertTrue(SecurityEnvironmentChecker.containsSuspiciousLibrary("/data/local/tmp/libsubstrate.so"))
    }

    @Test
    fun isSuspiciousThreadName_detectsFridaThreads() {
        listOf("gum-js-loop\n", "gmain", "gdbus", "pool-frida", "frida").forEach {
            assertTrue(it, SecurityEnvironmentChecker.isSuspiciousThreadName(it))
        }
    }

    @Test
    fun isSuspiciousThreadName_ignoresNormalAndroidThreads() {
        listOf("main\n", "RenderThread", "OkHttp Dispatcher", "pool-2-thread-1", "", "  ").forEach {
            assertFalse(it, SecurityEnvironmentChecker.isSuspiciousThreadName(it))
        }
    }

    @Test
    fun isTracerPidLine_detectsOnlyPositiveTracerPid() {
        assertTrue(SecurityEnvironmentChecker.isTracerPidLine("TracerPid:\t1234"))
        assertFalse(SecurityEnvironmentChecker.isTracerPidLine("TracerPid:\t0"))
        assertFalse(SecurityEnvironmentChecker.isTracerPidLine("TracerPid:"))
        assertFalse(SecurityEnvironmentChecker.isTracerPidLine("Pid:\t1234"))
    }

    @Test
    fun containsHookFrame_detectsXposedLsposedAndSubstrate() {
        assertTrue(SecurityEnvironmentChecker.containsHookFrame(listOf("a.B", "de.robv.android.xposed.XposedBridge")))
        assertTrue(SecurityEnvironmentChecker.containsHookFrame(listOf("org.lsposed.lspd.core.Main")))
        assertTrue(SecurityEnvironmentChecker.containsHookFrame(listOf("com.saurik.substrate.MS")))
        assertFalse(SecurityEnvironmentChecker.containsHookFrame(listOf("android.app.ActivityThread", "java.lang.Thread")))
    }

    @Test
    fun containsEmulatorMarker_detectsPcEmulators() {
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("generic", "goldfish")))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("sdk_gphone64_x86_64")))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("Genymotion", null)))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("BlueStacks")))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("google/vbox86p/vbox86p:9/PI/test:userdebug")))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("nox")))
        assertTrue(SecurityEnvironmentChecker.containsEmulatorMarker(listOf("Android/bst/bst:7.1")))
    }

    @Test
    fun containsEmulatorMarker_ignoresRealDevicesAndTvBoxes() {
        val realValues = listOf(
            "Xiaomi/mibox_global/mibox:9/PI/V1:user/release-keys", "MIBOX4", "Xiaomi",
            "Amazon/mantis/mantis:9/PS/1:user/release-keys", "AFTMM", "NVIDIA", "SHIELD Android TV",
            "mdarcy", "amlogic", "Realtek", "rtd299x", "S905X4", "Candy", "inox", "Samsung/dm1q/dm1q"
        )
        realValues.forEach {
            assertFalse(it, SecurityEnvironmentChecker.containsEmulatorMarker(listOf(it)))
        }
    }

    @Test
    fun isProxyConfigured_detectsEachProxySource() {
        assertTrue(SecurityEnvironmentChecker.isProxyConfigured("127.0.0.1", null, false))
        assertTrue(SecurityEnvironmentChecker.isProxyConfigured(null, "proxy.local", false))
        assertTrue(SecurityEnvironmentChecker.isProxyConfigured(null, null, true))
        assertFalse(SecurityEnvironmentChecker.isProxyConfigured(null, null, false))
        assertFalse(SecurityEnvironmentChecker.isProxyConfigured("", " ", false))
    }

    @Test
    fun isSuspiciousAppDirectory_acceptsStandardPaths() {
        val pkg = "com.soufcom.soukitv"

        assertFalse(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/user/0/$pkg/files", pkg))
        assertFalse(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/data/$pkg/files", pkg))
        assertFalse(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/user/10/$pkg/files", pkg))
    }

    @Test
    fun isSuspiciousAppDirectory_detectsClonesAndVirtualContainers() {
        val pkg = "com.soufcom.soukitv"

        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/user/999/$pkg/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/user/0/io.va.exposed/virtual/data/user/0/$pkg/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/data/com.lbe.parallel.intl/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/data/com.excelliance.dualaid/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/data/host/virtualapp/$pkg/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/data/data/host/files/$pkg/$pkg/files", pkg))
        assertTrue(SecurityEnvironmentChecker.isSuspiciousAppDirectory("/mnt/sdcard/clone/$pkg/files", pkg))
    }
}
