package com.stv.videoplayer.features.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

/** Effectue un test réseau croisé pour confirmer un blocage publicitaire réel. */
object AdBlockerDetector {
    suspend fun isAdBlockerActive(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!checkGeneralInternet(context)) return@withContext false
        checkGoogleAdDomainBlocked()
    }

    private fun checkGeneralInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true

        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return try {
                val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
                connection.connectTimeout = 1_500
                connection.readTimeout = 1_500
                connection.requestMethod = "HEAD"
                connection.responseCode in 200..399
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    private fun checkGoogleAdDomainBlocked(): Boolean {
        val domains = listOf(
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com"
        )
        for (domain in domains) {
            try {
                val host = InetAddress.getByName(domain).hostAddress
                if (host == "0.0.0.0" || host == "127.0.0.1" || host == "::1") return true
            } catch (_: UnknownHostException) {
                return true
            } catch (_: Exception) {
                // Une erreur DNS autre qu'une résolution négative ne confirme pas un blocage.
            }
        }
        return false
    }
}
