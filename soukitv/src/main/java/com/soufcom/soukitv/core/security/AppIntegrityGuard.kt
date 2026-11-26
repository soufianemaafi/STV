package com.soufcom.soukitv.core.security

import android.content.Context
import com.soufcom.soukitv.BuildConfig

/** Point de contrôle unique d'intégrité, injectable pour les tests JVM. */
fun interface AppIntegrityGuard {
    fun isTrusted(): Boolean
}

/** Signature de l'APK puis environnement ; l'environnement est ignoré en debug. */
class DefaultAppIntegrityGuard(private val context: Context) : AppIntegrityGuard {
    override fun isTrusted(): Boolean =
        AppSignatureVerifier.isGenuine(context) &&
            (BuildConfig.DEBUG || SecurityEnvironmentChecker.isEnvironmentSafe(context))
}
