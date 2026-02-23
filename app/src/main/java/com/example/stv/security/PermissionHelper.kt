package com.example.stv.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log

/**
 * Utilitaire pour vérifier les permissions et la signature de l'appelant.
 * Sécurise l'accès aux activités exportées.
 */
class PermissionHelper(private val context: Context) {

    private val TAG = "PermissionHelper"

    /**
     * Vérifie si l'app appelante a la permission requise et une signature valide.
     * @param callerPackage Le package name de l'app appelante
     * @param requiredPermission La permission requise (ex. "com.example.stv.PERMISSION_LAUNCH_PLAYER")
     * @return True si autorisée, False sinon
     */
    fun isCallerAuthorized(callerPackage: String?, requiredPermission: String): Boolean {
        if (callerPackage.isNullOrEmpty()) {
            Log.w(TAG, "Caller package is null or empty")
            return false
        }

        // Vérifier que l'app appelante a la permission
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

            // Bonus : vérifier la signature (optionnel mais recommandé)
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
     * Vérifie la signature de l'app appelante (protection supplémentaire).
     * Les apps signées avec la même clé que STV Player sont autorisées.
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

            // Vérifier que la signature correspond à celle de STV Player
            val ourSignature = getOurAppSignature()
            val callerSignature = signatures[0]

            val isValid = ourSignature == callerSignature.toByteArray()
            if (isValid) {
                Log.d(TAG, "Signature verification passed for $pkgName")
            } else {
                Log.w(TAG, "Signature mismatch for $pkgName")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying signature: ${e.message}", e)
            // Fallback : si la vérification échoue, refuser
            false
        }
    }

    /**
     * Récupère la signature de notre app (STV Player).
     * Utilise le package manager pour accéder aux infos de signature.
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

    /**
     * Extension pour convertir Signature en ByteArray.
     */
    private fun Signature.toByteArray(): ByteArray = this.toByteArray()
}

