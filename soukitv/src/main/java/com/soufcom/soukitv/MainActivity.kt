package com.soufcom.soukitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.soufcom.soukitv.ui.home.HomeScreen
import com.soufcom.soukitv.ui.theme.SoukiTVTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import com.soufcom.soukitv.features.home.presentation.HomeUiState
import com.soufcom.soukitv.features.home.presentation.HomeViewModel
import com.soufcom.soukitv.features.home.presentation.HomeViewModelFactory
import com.soufcom.soukitv.core.security.AppIntegrityGuard
import com.soufcom.soukitv.core.security.DefaultAppIntegrityGuard
import android.os.Process

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(applicationContext)
    }

    private val integrityGuard: AppIntegrityGuard by lazy {
        DefaultAppIntegrityGuard(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!enforceIntegrity()) return
        setContent {
            val uiState by homeViewModel.uiState.collectAsState()
            val isDarkTheme = (uiState as? HomeUiState.Success)?.isDarkTheme ?: true

            SoukiTVTheme(darkTheme = isDarkTheme) {
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }

    // Re-contrôle à chaque retour au premier plan : couvre un attachement tardif (Frida, debugger).
    override fun onResume() {
        super.onResume()
        enforceIntegrity()
    }

    private fun enforceIntegrity(): Boolean {
        if (integrityGuard.isTrusted()) return true
        finishAffinity()
        Process.killProcess(Process.myPid())
        System.exit(1)
        return false
    }
}
