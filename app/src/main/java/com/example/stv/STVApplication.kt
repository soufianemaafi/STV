package com.example.stv

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * Classe Application globale pour STV Player.
 *
 * Responsabilités :
 * - Initialisation unique du SDK AdMob (MobileAds.initialize)
 * - Initialisation unique du singleton AdManager (avec préchargement automatique)
 *
 * Le flux publicitaire est orchestré par `AdFlowController` au moment où
 * `PlayerActivity` en a besoin → un seul flux linéaire.
 */
class STVApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialisation unique du SDK AdMob pour toute l'application
        MobileAds.initialize(this) {
            Log.d("STVApplication", "AdMob SDK initialized successfully")
        }

        // ✅ Initialisation du singleton AdManager : cela lance aussi le préchargement
        // d'une interstitielle pour garantir un zapping rapide.
        AdManager.initialize(this)
    }
}

