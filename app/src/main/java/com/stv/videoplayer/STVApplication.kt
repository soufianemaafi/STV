package com.stv.videoplayer

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * Classe Application globale pour STV Player.
 *
 * ResponsabilitÃ©s :
 * - Initialisation unique du SDK AdMob (MobileAds.initialize)
 * - Initialisation unique du singleton AdManager (avec prÃ©chargement automatique)
 *
 * Le flux publicitaire est orchestrÃ© par `AdFlowController` au moment oÃ¹
 * `PlayerActivity` en a besoin â†’ un seul flux linÃ©aire.
 */
class STVApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // âœ… Initialisation unique du SDK AdMob pour toute l'application
        MobileAds.initialize(this) {
            Log.d("STVApplication", "AdMob SDK initialized successfully")
        }

        // âœ… Initialisation du singleton AdManager : cela lance aussi le prÃ©chargement
        // d'une interstitielle pour garantir un zapping rapide.
        AdManager.initialize(this)
    }
}


