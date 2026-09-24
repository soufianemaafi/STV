package com.stv.videoplayer.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utilitaire rÃ©seau â€” point unique pour vÃ©rifier la connectivitÃ©.
 * UtilisÃ© par AdManager, AdsController, MainActivity, etc.
 */
object NetworkUtils {

    /**
     * VÃ©rifie si une connexion internet est disponible et validÃ©e.
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


