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
import com.soufcom.soukitv.core.security.AppSignatureVerifier
import com.soufcom.soukitv.core.security.SecurityEnvironmentChecker
import com.soufcom.soukitv.BuildConfig

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!AppSignatureVerifier.isGenuine(this)) {
            finishAffinity()
            return
        }
        if (!BuildConfig.DEBUG && !SecurityEnvironmentChecker.isEnvironmentSafe(this)) {
            finishAffinity()
            System.exit(0)
            return
        }
        setContent {
            val uiState by homeViewModel.uiState.collectAsState()
            val isDarkTheme = (uiState as? HomeUiState.Success)?.isDarkTheme ?: true

            SoukiTVTheme(darkTheme = isDarkTheme) {
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }
}
