package com.stv.videoplayer.features.home.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.stv.videoplayer.core.security.IntentSecurityManager
import com.stv.videoplayer.core.security.IntentValidationResult
import com.stv.videoplayer.navigation.STVNavGraph
import com.stv.videoplayer.ui.theme.STVTheme

/**
 * Point d'entrÃ©e unique de l'application.
 *
 * HÃ©berge le NavHost Compose avec 3 destinations :
 *   Home â†’ Videos â†’ AddVideo
 *
 * PlayerActivity reste une Activity sÃ©parÃ©e (PiP, singleTask, deep links).
 *
 * Le VideoListViewModel est crÃ©Ã© ici et partagÃ© entre tous les Ã©crans
 * via le NavGraph â†’ plus de communication par Intent/ActivityResult.
 */
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private var keepSplashScreen = true
    private val videoListViewModel: VideoListViewModel by viewModels()

    // âœ… SÃ©curitÃ© : MainActivity est `exported="true"` (LAUNCHER), donc n'importe
    // quelle app peut lui envoyer un Intent arbitraire. On valide systÃ©matiquement
    // tout contenu "vidÃ©o" potentiellement prÃ©sent (extras/data) avant d'y toucher,
    // mÃªme si cet Ã©cran ne consomme normalement aucune URL vidÃ©o directement.
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

        // Retirer le splash screen aprÃ¨s 500ms
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
     * Valide l'Intent entrant via [IntentSecurityManager] en toute sÃ©curitÃ©.
     * MainActivity ne consomme actuellement aucune URL vidÃ©o : cette vÃ©rification
     * est une protection dÃ©fensive contre l'Intent Hijacking (Activity exportÃ©e).
     * Aucun crash possible, aucune action mÃ©tier ici â€” uniquement du logging.
     */
    private fun validateIncomingIntentSafely(intent: Intent?) {
        when (val result = intentSecurityManager.validateIncomingIntent(intent)) {
            is IntentValidationResult.Valid -> {
                Log.d(tag, "Intent entrant validÃ© (non utilisÃ© par MainActivity)")
            }
            is IntentValidationResult.Invalid -> {
                Log.w(tag, "Intent entrant ignorÃ© : ${result.reason}")
            }
        }
    }
}


