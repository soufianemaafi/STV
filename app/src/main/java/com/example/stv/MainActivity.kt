package com.example.stv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
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

    private var keepSplashScreen = true
    private val videoListViewModel: VideoListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installer le SplashScreen avant super.onCreate()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

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
}
