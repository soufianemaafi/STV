package com.stv.videoplayer.security
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
/** Signature-based authorization for exported player entry points and sensitive extras. */
class PermissionHelper(private val context: Context) {
    private companion object {
        const val TAG = "PermissionHelper"
    }
    /** Allows normal playback for system/deep-link intents; identifiable callers need same signing certificate. */
    fun isCallerAuthorizedForPlayer(callerPackage: String?): Boolean {
        if (callerPackage.isNullOrBlank()) {
            Log.d(TAG, "Caller package unavailable; allowing non-sensitive playback only")
            return true
        }
        return hasSameSigningCertificate(callerPackage)
    }
    /** Sensitive extras require an identifiable, same-signed caller. */
    fun isCallerAuthorizedForSensitiveExtras(callerPackage: String?): Boolean {
        return !callerPackage.isNullOrBlank() && hasSameSigningCertificate(callerPackage)
    }
    /** Both the declared permission and the signing certificate are required. */
    fun isCallerAuthorized(callerPackage: String?, requiredPermission: String): Boolean {
        if (callerPackage.isNullOrBlank()) return false
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                callerPackage,
                PackageManager.GET_PERMISSIONS
            )
            val hasPermission = packageInfo.requestedPermissions?.contains(requiredPermission) == true
            hasPermission && hasSameSigningCertificate(callerPackage)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to verify caller permission", e)
            false
        }
    }
    private fun hasSameSigningCertificate(packageName: String): Boolean {
        return try {
            val packageManager = context.packageManager
            val callerSignatures = getSignatures(
                packageManager.getPackageInfo(packageName, signingInfoFlag())
            )
            val ownSignatures = getSignatures(
                packageManager.getPackageInfo(context.packageName, signingInfoFlag())
            )
            val matches = callerSignatures.any { caller ->
                ownSignatures.any { own -> caller.contentEquals(own) }
            }
            if (!matches) Log.w(TAG, "Signing certificate mismatch for $packageName")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Unable to verify signing certificate for $packageName", e)
            false
        }
    }
    private fun signingInfoFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
    }
    private fun getSignatures(packageInfo: PackageInfo): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.toByteArray() }.orEmpty()
        }
    }
}