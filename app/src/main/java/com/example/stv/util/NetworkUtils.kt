package com.example.stv.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utilitaire réseau — point unique pour vérifier la connectivité.
 * Utilisé par AdManager, AdsController, MainActivity, etc.
 */
object NetworkUtils {

    /**
     * Vérifie si une connexion internet est disponible et validée.
     * Utilise l'API moderne NetworkCapabilities (depuis minSdk=24).
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

