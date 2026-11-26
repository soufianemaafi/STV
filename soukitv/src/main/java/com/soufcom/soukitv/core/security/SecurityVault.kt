package com.soufcom.soukitv.core.security

import android.content.Context

interface SecurityVault {
    fun nativeVerifyIntegrity(): Boolean

    fun remoteUrl(key: Int): String

    fun trustedHosts(): Set<String>

    fun playerDigests(): List<ByteArray>
}

object UnavailableSecurityVault : SecurityVault {
    override fun nativeVerifyIntegrity(): Boolean = false

    override fun remoteUrl(key: Int): String = ""

    override fun trustedHosts(): Set<String> = emptySet()

    override fun playerDigests(): List<ByteArray> = emptyList()
}

internal object NativeSecurityBridge {
    internal val isLoaded: Boolean

    init {
        isLoaded = runCatching {
            System.loadLibrary("stvsecurity")
            true
        }.getOrDefault(false)
    }

    @JvmStatic
    external fun nativeVerifyIntegrity(context: Context): Boolean

    @JvmStatic
    external fun nativeGetRemoteUrl(context: Context, key: Int): String

    @JvmStatic
    external fun nativeGetTrustedHosts(context: Context): Array<String>

    @JvmStatic
    external fun nativeGetPlayerDigests(context: Context): Array<ByteArray>
}

class NativeSecurityVault(context: Context) : SecurityVault {
    private val appContext = context.applicationContext

    override fun nativeVerifyIntegrity(): Boolean =
        if (!NativeSecurityBridge.isLoaded) {
            false
        } else {
            runCatching { NativeSecurityBridge.nativeVerifyIntegrity(appContext) }
                .getOrDefault(false)
        }

    override fun remoteUrl(key: Int): String =
        if (!nativeVerifyIntegrity()) {
            ""
        } else {
            runCatching { NativeSecurityBridge.nativeGetRemoteUrl(appContext, key) }
                .getOrDefault("")
        }

    override fun trustedHosts(): Set<String> =
        if (!nativeVerifyIntegrity()) {
            emptySet()
        } else {
            runCatching { NativeSecurityBridge.nativeGetTrustedHosts(appContext).toSet() }
                .getOrDefault(emptySet())
        }

    override fun playerDigests(): List<ByteArray> =
        if (!nativeVerifyIntegrity()) {
            emptyList()
        } else {
            runCatching { NativeSecurityBridge.nativeGetPlayerDigests(appContext).toList() }
                .getOrDefault(emptyList())
        }
}
