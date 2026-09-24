package com.stv.videoplayer.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log

/**
 * Utilitaire pour vÃ©rifier les permissions et la signature de l'appelant.
 * SÃ©curise l'accÃ¨s aux activitÃ©s exportÃ©es.
 */
class PermissionHelper(private val context: Context) {

    private val TAG = "PermissionHelper"

    /**
     * VÃ©rifie si l'app appelante est autorisÃ©e Ã  lancer le player.
     * Autorise : apps signÃ©es avec la mÃªme clÃ© + intents systÃ¨me.
     * @param callerPackage Le package name de l'app appelante (null si intent systÃ¨me)
     * @return True si autorisÃ©e, False sinon
     */
    fun isCallerAuthorizedForPlayer(callerPackage: String?): Boolean {
        // Si pas d'appelant (intent systÃ¨me/deep link), autoriser
        if (callerPackage.isNullOrEmpty()) {
            Log.d(TAG, "System intent or deep link, allowing access")
            return true
        }

        // Si l'appelant est le systÃ¨me Android (ex: navigateur, file manager)
        if (callerPackage == "android" || callerPackage.startsWith("com.android.")) {
            Log.d(TAG, "Android system caller, allowing access")
            return true
        }

        // Si l'appelant est nous-mÃªme (STV Player)
        if (callerPackage == context.packageName) {
            Log.d(TAG, "Self-call, allowing access")
            return true
        }

        // VÃ©rifier la signature (apps catalogue signÃ©es avec la mÃªme clÃ©)
        return verifySignature(callerPackage)
    }

    /**
     * VÃ©rifie si l'app appelante a la permission requise et une signature valide.
     * @param callerPackage Le package name de l'app appelante
     * @param requiredPermission La permission requise (ex. "com.stv.videoplayer.PERMISSION_LAUNCH_PLAYER")
     * @return True si autorisÃ©e, False sinon
     */
    fun isCallerAuthorized(callerPackage: String?, requiredPermission: String): Boolean {
        if (callerPackage.isNullOrEmpty()) {
            Log.w(TAG, "Caller package is null or empty")
            return false
        }

        // VÃ©rifier que l'app appelante a la permission
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(
                callerPackage,
                PackageManager.GET_PERMISSIONS
            )

            val hasPermission = pkgInfo.requestedPermissions?.contains(requiredPermission) == true
            if (!hasPermission) {
                Log.w(TAG, "Caller $callerPackage missing required permission: $requiredPermission")
                return false
            }

            // Bonus : vÃ©rifier la signature (optionnel mais recommandÃ©)
            val isSignatureValid = verifySignature(callerPackage)
            if (!isSignatureValid) {
                Log.w(TAG, "Caller $callerPackage has invalid signature")
            }

            isSignatureValid
        } catch (e: Exception) {
            Log.e(TAG, "Error checking caller authorization: ${e.message}", e)
            false
        }
    }

    /**
     * VÃ©rifie la signature de l'app appelante (protection supplÃ©mentaire).
     * Les apps signÃ©es avec la mÃªme clÃ© que STV Player sont autorisÃ©es.
     */
    private fun verifySignature(pkgName: String): Boolean {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    pkgName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    pkgName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners ?: arrayOf()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures ?: arrayOf()
            }

            if (signatures.isEmpty()) {
                Log.w(TAG, "No signatures found for $pkgName")
                return false
            }

            // VÃ©rifier que la signature correspond Ã  celle de STV Player
            val ourSignature = getOurAppSignature()
            val callerSignature = signatures[0].toByteArray()

            val isValid = ourSignature.contentEquals(callerSignature)
            if (isValid) {
                Log.d(TAG, "Signature verification passed for $pkgName")
            } else {
                Log.w(TAG, "Signature mismatch for $pkgName")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying signature: ${e.message}", e)
            // Fallback : si la vÃ©rification Ã©choue, refuser
            false
        }
    }

    /**
     * RÃ©cupÃ¨re la signature de notre app (STV Player).
     * Utilise le package manager pour accÃ©der aux infos de signature.
     */
    private fun getOurAppSignature(): ByteArray {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners ?: arrayOf()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures ?: arrayOf()
            }

            if (signatures.isNotEmpty()) {
                signatures[0].toByteArray()
            } else {
                ByteArray(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting our app signature: ${e.message}")
            ByteArray(0)
        }
    }
}


