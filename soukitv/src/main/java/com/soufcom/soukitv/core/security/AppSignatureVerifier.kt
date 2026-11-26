package com.soufcom.soukitv.core.security

import android.content.Context
import com.soufcom.soukitv.BuildConfig
import java.security.MessageDigest

object AppSignatureVerifier {
    fun isGenuine(
        context: Context,
        securityVault: SecurityVault = NativeSecurityVault(context)
    ): Boolean {
        if (BuildConfig.DEBUG) return true
        return securityVault.nativeVerifyIntegrity()
    }

    internal fun isDigestEqual(signatureBytes: ByteArray, expectedDigest: ByteArray): Boolean {
        val actualDigest = MessageDigest.getInstance("SHA-256").digest(signatureBytes)
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }
}
