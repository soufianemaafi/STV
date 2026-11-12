package com.soufcom.soukitv.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File

/**
 * Contrôles locaux de réduction du risque sur les appareils instrumentés ou compromis.
 *
 * Ces contrôles ne constituent pas une preuve d'intégrité : un attaquant rooté peut les
 * contourner. L'autorisation réelle des flux doit rester vérifiée côté serveur.
 */
object SecurityEnvironmentChecker {
    private const val FRIDA_DEFAULT_PORT = 27042

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/xbin/su",
        "/system/sd/xbin/su"
    )

    private val hostilePackages = setOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "com.koushikdutta.superuser",
        "stericson.busybox",
        "ru.meefik.busybox"
    )

    private val suspiciousMapTokens = setOf(
        "frida-agent.so",
        "frida-gadget.so",
        "libfrida",
        "xposed"
    )

    fun isEnvironmentSafe(context: Context): Boolean =
        !hasSuBinary() &&
            !hasHostilePackage(context) &&
            !hasTestKeys() &&
            !isDebuggerAttached() &&
            !isApplicationDebuggable(context) &&
            !hasFridaPort() &&
            !hasSuspiciousMappedLibrary()

    private fun hasSuBinary(): Boolean = suPaths.any { path ->
        runCatching { File(path).exists() }.getOrDefault(false)
    }

    private fun hasHostilePackage(context: Context): Boolean {
        val packageManager = context.packageManager
        return hostilePackages.any { packageName ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }
                true
            }.getOrDefault(false)
        }
    }

    private fun hasTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun isDebuggerAttached(): Boolean = runCatching {
        Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }.getOrDefault(true)

    private fun isApplicationDebuggable(context: Context): Boolean =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    private fun hasFridaPort(): Boolean =
        hasListeningPort("/proc/net/tcp") || hasListeningPort("/proc/net/tcp6")

    private fun hasListeningPort(path: String): Boolean = runCatching {
        File(path).useLines { lines ->
            lines.drop(1).any(::isFridaPortLine)
        }
    }.getOrDefault(false)

    private fun hasSuspiciousMappedLibrary(): Boolean = runCatching {
        File("/proc/self/maps").useLines { lines ->
            lines.any(::containsSuspiciousLibrary)
        }
    }.getOrDefault(false)

    internal fun isFridaPortLine(line: String): Boolean {
        val columns = line.trim().split(Regex("\\s+"))
        val localAddress = columns.getOrNull(1).orEmpty()
        val state = columns.getOrNull(3).orEmpty()
        return localAddress.substringAfterLast(':', "")
            .toIntOrNull(16) == FRIDA_DEFAULT_PORT && state == "0A"
    }

    internal fun containsSuspiciousLibrary(line: String): Boolean {
        val normalized = line.lowercase()
        return suspiciousMapTokens.any(normalized::contains)
    }
}

