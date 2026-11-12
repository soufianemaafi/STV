package com.soufcom.soukitv.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.soufcom.soukitv.BuildConfig
import java.security.MessageDigest

object AppSignatureVerifier {

    private val MASKED_DIGEST = byteArrayOf(
        0x03, 0x01, 0x3F, 0x75, 0x66, 0xDA.toByte(), 0x1B, 0xD3.toByte(),
        0x42, 0x30, 0x42, 0xFA.toByte(), 0x92.toByte(), 0xE0.toByte(), 0xCC.toByte(), 0x71,
        0xF9.toByte(), 0x1B, 0x4C, 0x14, 0x1E, 0xB4.toByte(), 0x71, 0x7D,
        0xE2.toByte(), 0xCA.toByte(), 0x0E, 0xD1.toByte(), 0x60, 0xB4.toByte(), 0xEC.toByte(), 0xDF.toByte()
    )

    private const val XOR_KEY: Byte = 0x5A

    fun isGenuine(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true

        return try {
            val expectedDigest = MASKED_DIGEST
                .map { (it.toInt() xor XOR_KEY.toInt()).toByte() }
                .toByteArray()
            val signatures = packageSignatures(context)

            signatures.any { signature ->
                isDigestEqual(signature.toByteArray(), expectedDigest)
            }
        } catch (_: Exception) {
            false
        }
    }

    internal fun isDigestEqual(signatureBytes: ByteArray, expectedDigest: ByteArray): Boolean {
        val actualDigest = MessageDigest.getInstance("SHA-256").digest(signatureBytes)
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }

    private fun packageSignatures(context: Context): Array<Signature> {
        val packageManager = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures ?: emptyArray()
        }
    }
}

