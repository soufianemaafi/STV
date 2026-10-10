package com.example.soukitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.soukitv.ui.home.HomeScreen
import com.example.soukitv.ui.theme.SoukiTVTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import com.example.soukitv.features.home.presentation.HomeUiState
import com.example.soukitv.features.home.presentation.HomeViewModel
import com.example.soukitv.features.home.presentation.HomeViewModelFactory

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by homeViewModel.uiState.collectAsState()
            val isDarkTheme = (uiState as? HomeUiState.Success)?.isDarkTheme ?: true

            SoukiTVTheme(darkTheme = isDarkTheme) {
                HomeScreen(viewModel = homeViewModel)
            }
        }
    }
}
