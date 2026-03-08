package com.example.stv

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

/**
 * Classe Application globale pour STV Player.
 *
 * Responsabilités :
 * - Initialisation unique du SDK AdMob (MobileAds.initialize)
 * - Initialisation unique du singleton AdManager (PAS de préchargement)
 *
 * Le chargement de la pub se fait dans AdsController.showAdIfNeeded()
 * au moment où PlayerActivity en a besoin → un seul flux linéaire.
 */
class STVApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialisation unique du SDK AdMob pour toute l'application
        MobileAds.initialize(this) {
            Log.d("STVApplication", "AdMob SDK initialized successfully")
        }

        // ✅ Initialisation du singleton AdManager (PAS de préchargement ici)
        // Le chargement de la pub se fera dans AdsController.showAdIfNeeded()
        // Cela évite les doublons de pub (préchargée + chargée à nouveau)
        AdManager.initialize(this)
    }
}

