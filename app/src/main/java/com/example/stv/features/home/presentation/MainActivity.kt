package com.example.stv.features.home.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.stv.core.security.IntentSecurityManager
import com.example.stv.core.security.IntentValidationResult
import com.example.stv.navigation.STVNavGraph
import com.example.stv.ui.theme.STVTheme

/**
 * Point d'entrée unique de l'application.
 *
 * Héberge le NavHost Compose avec 3 destinations :
 *   Home → Videos → AddVideo
 *
 * PlayerActivity reste une Activity séparée (PiP, singleTask, deep links).
 *
 * Le VideoListViewModel est créé ici et partagé entre tous les écrans
 * via le NavGraph → plus de communication par Intent/ActivityResult.
 */
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private var keepSplashScreen = true
    private val videoListViewModel: VideoListViewModel by viewModels()

    // ✅ Sécurité : MainActivity est `exported="true"` (LAUNCHER), donc n'importe
    // quelle app peut lui envoyer un Intent arbitraire. On valide systématiquement
    // tout contenu "vidéo" potentiellement présent (extras/data) avant d'y toucher,
    // même si cet écran ne consomme normalement aucune URL vidéo directement.
    private val intentSecurityManager = IntentSecurityManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installer le SplashScreen avant super.onCreate()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

        validateIncomingIntentSafely(intent)

        setContent {
            STVTheme {
                val navController = rememberNavController()
                STVNavGraph(
                    navController = navController,
                    videoListViewModel = videoListViewModel
                )
            }
        }

        // Retirer le splash screen après 500ms
        window.decorView.postDelayed({
            keepSplashScreen = false
        }, 500)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        validateIncomingIntentSafely(intent)
    }

    /**
     * Valide l'Intent entrant via [IntentSecurityManager] en toute sécurité.
     * MainActivity ne consomme actuellement aucune URL vidéo : cette vérification
     * est une protection défensive contre l'Intent Hijacking (Activity exportée).
     * Aucun crash possible, aucune action métier ici — uniquement du logging.
     */
    private fun validateIncomingIntentSafely(intent: Intent?) {
        when (val result = intentSecurityManager.validateIncomingIntent(intent)) {
            is IntentValidationResult.Valid -> {
                Log.d(tag, "Intent entrant validé (non utilisé par MainActivity)")
            }
            is IntentValidationResult.Invalid -> {
                Log.w(tag, "Intent entrant ignoré : ${result.reason}")
            }
        }
    }
}

