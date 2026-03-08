package com.example.stv

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * Classe Application globale pour STV Player.
 *
 * Responsabilités :
 * - Initialisation unique du SDK AdMob (MobileAds.initialize)
 *   Cela garantit que le SDK est prêt AVANT le lancement de n'importe quelle Activity,
 *   que l'utilisateur arrive via le launcher (MainActivity) ou via un deep link (PlayerActivity).
 *
 * Avantages :
 * - Élimine le doublon MobileAds.initialize() dans MainActivity et PlayerActivity
 * - Fonctionne pour TOUS les points d'entrée (launcher, deep link, intent catalogue)
 * - Le SDK est initialisé une seule fois pour tout le cycle de vie de l'app
 */
class STVApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialisation unique du SDK AdMob pour toute l'application
        // Cet appel est garanti d'être exécuté AVANT tout onCreate() d'Activity
        MobileAds.initialize(this) {
            Log.d("STVApplication", "AdMob SDK initialized successfully")
        }
    }
}

