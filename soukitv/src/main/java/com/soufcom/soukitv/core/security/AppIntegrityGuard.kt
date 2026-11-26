package com.soufcom.soukitv.core.security

import android.content.Context
import com.soufcom.soukitv.BuildConfig

/** Point de contrôle unique d'intégrité, injectable pour les tests JVM. */
fun interface AppIntegrityGuard {
    fun isTrusted(): Boolean
}

/** Signature de l'APK puis environnement ; l'environnement est ignoré en debug. */
class DefaultAppIntegrityGuard(
    private val context: Context,
    private val securityVault: SecurityVault = NativeSecurityVault(context)
) : AppIntegrityGuard {
    override fun isTrusted(): Boolean {
        val nativeTrusted = securityVault.nativeVerifyIntegrity()
        if (BuildConfig.DEBUG) return true
        return nativeTrusted &&
            AppSignatureVerifier.isGenuine(context, securityVault) &&
            SecurityEnvironmentChecker.isEnvironmentSafe(context)
    }
}
