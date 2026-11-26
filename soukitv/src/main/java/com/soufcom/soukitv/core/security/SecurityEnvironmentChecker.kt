package com.soufcom.soukitv.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Debug
import java.io.File

enum class SecurityAssessment { SAFE, NETWORK_ENVIRONMENT_BLOCKED, COMPROMISED_SILENT_EXIT }

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

    // Outils root, hooks et édition d'APK : fermeture silencieuse.
    private val tamperAndRootPackages = setOf(
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "com.koushikdutta.superuser",
        "stericson.busybox",
        "ru.meefik.busybox",
        "org.meowcat.edxposed.manager",
        "org.lsposed.manager",
        "com.chelpus.lackypatch",
        "com.dimonvideo.luckypatcher",
        "com.forpda.lp",
        "ru.sxbuido.luckypatcher",
        "bin.mt.plus",
        "bin.mt.plus.canary"
    )

    // Capture / proxy réseau : écran générique si l'appareil est sain.
    private val networkCapturePackages = setOf(
        "com.emanuelef.remote_capture",
        "com.guoshi.httpcanary",
        "com.guoshi.httpcanary.premium",
        "app.greyshirts.sslcapture",
        "com.minhui.networkcapture",
        "com.egorovandreyrm.pcapremote",
        "com.reqable.android",
        "com.network.proxy",
        "tech.httptoolkit.android.v1",
        "jp.co.taosoftware.android.packetcapture"
    )

    private val hostilePackages = tamperAndRootPackages + networkCapturePackages

    private val suspiciousMapTokens = setOf(
        "frida-agent.so",
        "frida-gadget.so",
        "libfrida",
        "xposed",
        "lsposed",
        "lspd",
        "riru",
        "zygisk",
        "substrate",
        "edxp",
        "magisk"
    )

    private val exactSuspiciousThreadNames = setOf("gum-js-loop", "gmain", "gdbus")

    private val hookClassNames = listOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers",
        "org.lsposed.lspd.core.Main",
        "com.saurik.substrate.MS"
    )

    private val hookFrameMarkers = listOf(
        "de.robv.android.xposed",
        "org.lsposed.lspd",
        "com.saurik.substrate"
    )

    // Marqueurs d'émulateurs PC uniquement : aucun terme générique des Android TV Box.
    private val emulatorSubstringMarkers = listOf(
        "goldfish", "ranchu", "vbox86", "bluestacks", "ldplayer", "droid4x",
        "genymotion", "google_sdk", "sdk_gphone", "emulator", "simulator"
    )

    // Marqueurs courts : comparés mot entier pour éviter les faux positifs de sous-chaîne.
    private val emulatorTokenMarkers = setOf("nox", "bst", "memu", "andy")

    private val virtualContainerMarkers = listOf(
        "io.va.exposed", "com.lbe.parallel", "com.excelliance.dualaid", "virtualapp"
    )

    private val standardDataPrefixes = listOf("/data/user/", "/data/data/")

    fun isEnvironmentSafe(context: Context): Boolean =
        assessEnvironment(context) == SecurityAssessment.SAFE

    fun assessEnvironment(context: Context): SecurityAssessment {
        val severeThreat = hasSuBinary() ||
            hasAnyPackage(context, tamperAndRootPackages) ||
            hasTestKeys() ||
            isDebuggerAttached() ||
            isApplicationDebuggable(context) ||
            hasFridaPort() ||
            hasSuspiciousMappedLibrary() ||
            hasSuspiciousThread() ||
            isTracerAttached() ||
            hasHookFramework() ||
            isPcEmulator() ||
            isClonedOrVirtualized(context)
        if (severeThreat) return SecurityAssessment.COMPROMISED_SILENT_EXIT
        if (isSystemProxyActive(context) || hasAnyPackage(context, networkCapturePackages)) {
            return SecurityAssessment.NETWORK_ENVIRONMENT_BLOCKED
        }
        return SecurityAssessment.SAFE
    }

    private fun hasSuBinary(): Boolean = suPaths.any { path ->
        runCatching { File(path).exists() }.getOrDefault(false)
    }

    internal fun hasHostilePackage(context: Context): Boolean =
        hasAnyPackage(context, hostilePackages)

    private fun hasAnyPackage(context: Context, packages: Set<String>): Boolean {
        val packageManager = context.packageManager
        return packages.any { packageName ->
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

    private fun isSystemProxyActive(context: Context): Boolean {
        val hasDefaultProxy = runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.defaultProxy != null
        }.getOrDefault(false)
        val httpHost = runCatching { System.getProperty("http.proxyHost") }.getOrNull()
        val httpsHost = runCatching { System.getProperty("https.proxyHost") }.getOrNull()
        return isProxyConfigured(httpHost, httpsHost, hasDefaultProxy)
    }

    private fun hasSuspiciousThread(): Boolean = runCatching {
        File("/proc/self/task").listFiles()?.any { taskDir ->
            runCatching { isSuspiciousThreadName(File(taskDir, "comm").readText()) }
                .getOrDefault(false)
        } ?: false
    }.getOrDefault(false)

    private fun isTracerAttached(): Boolean = runCatching {
        File("/proc/self/status").useLines { lines -> lines.any(::isTracerPidLine) }
    }.getOrDefault(false)

    private fun hasHookFramework(): Boolean {
        val loader = SecurityEnvironmentChecker::class.java.classLoader
        val classLoaded = hookClassNames.any { name ->
            runCatching { Class.forName(name, false, loader); true }.getOrDefault(false)
        }
        val frameDetected = runCatching {
            containsHookFrame(Throwable().stackTrace.map { it.className })
        }.getOrDefault(false)
        return classLoaded || frameDetected
    }

    private fun isPcEmulator(): Boolean = runCatching {
        containsEmulatorMarker(
            listOf(
                Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER, Build.BRAND,
                Build.DEVICE, Build.PRODUCT, Build.HARDWARE
            )
        )
    }.getOrDefault(false)

    private fun isClonedOrVirtualized(context: Context): Boolean = runCatching {
        isSuspiciousAppDirectory(context.filesDir.absolutePath, context.packageName)
    }.getOrDefault(false)

    internal fun isProxyConfigured(
        httpProxyHost: String?,
        httpsProxyHost: String?,
        hasDefaultProxy: Boolean
    ): Boolean = hasDefaultProxy ||
        !httpProxyHost.isNullOrBlank() ||
        !httpsProxyHost.isNullOrBlank()

    internal fun isSuspiciousThreadName(rawName: String): Boolean {
        val name = rawName.trim().lowercase()
        if (name.isEmpty()) return false
        return name in exactSuspiciousThreadNames || name.contains("frida")
    }

    internal fun isTracerPidLine(line: String): Boolean {
        if (!line.startsWith("TracerPid:")) return false
        return (line.substringAfter(':').trim().toLongOrNull() ?: 0L) > 0L
    }

    internal fun containsHookFrame(classNames: List<String>): Boolean =
        classNames.any { className -> hookFrameMarkers.any(className::startsWith) }

    internal fun containsEmulatorMarker(values: List<String?>): Boolean =
        values.filterNotNull().map(String::lowercase).any { value ->
            emulatorSubstringMarkers.any(value::contains) ||
                value.split(Regex("[^a-z0-9]+")).any { it in emulatorTokenMarkers }
        }

    internal fun isSuspiciousAppDirectory(path: String, packageName: String): Boolean {
        val normalized = path.lowercase()
        if (standardDataPrefixes.none(normalized::startsWith)) return true
        if (virtualContainerMarkers.any(normalized::contains)) return true
        if ("999" in normalized.split('/')) return true
        if (packageName.isNotEmpty() && normalized.split(packageName.lowercase()).size - 1 > 1) {
            return true
        }
        return Regex("/data/(user|data)/").findAll(normalized).count() > 1
    }

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
