package com.soufcom.soukitv

import android.content.Context
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.soufcom.soukitv.core.security.AppSignatureVerifier
import com.soufcom.soukitv.core.security.NativeSecurityVault
import com.soufcom.soukitv.core.security.SecurityAssessment
import com.soufcom.soukitv.core.security.SecurityEnvironmentChecker
import com.soufcom.soukitv.features.home.presentation.HomeUiState
import com.soufcom.soukitv.features.home.presentation.HomeViewModel
import com.soufcom.soukitv.features.home.presentation.HomeViewModelFactory
import com.soufcom.soukitv.features.home.presentation.components.NetworkBlockedScreen
import com.soufcom.soukitv.ui.home.HomeScreen
import com.soufcom.soukitv.ui.theme.SoukiTVTheme

internal fun evaluateAccess(context: Context): SecurityAssessment {
    val securityVault = NativeSecurityVault(context)
    if (!BuildConfig.DEBUG && !securityVault.nativeVerifyIntegrity()) {
        return SecurityAssessment.COMPROMISED_SILENT_EXIT
    }
    return when {
        !AppSignatureVerifier.isGenuine(context, securityVault) ->
            SecurityAssessment.COMPROMISED_SILENT_EXIT
        BuildConfig.DEBUG -> SecurityAssessment.SAFE
        else -> SecurityEnvironmentChecker.assessEnvironment(context)
    }
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(applicationContext)
    }

    // Fail-closed : aucun accès au ViewModel (donc aucun réseau) tant que l'évaluation n'est pas SAFE.
    private var accessState by mutableStateOf(SecurityAssessment.NETWORK_ENVIRONMENT_BLOCKED)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!reevaluate()) return
        setContent {
            when (accessState) {
                SecurityAssessment.SAFE -> {
                    val uiState by homeViewModel.uiState.collectAsState()
                    val isDarkTheme = (uiState as? HomeUiState.Success)?.isDarkTheme ?: true
                    SoukiTVTheme(darkTheme = isDarkTheme) {
                        HomeScreen(viewModel = homeViewModel)
                    }
                }
                else -> SoukiTVTheme(darkTheme = true) {
                    NetworkBlockedScreen(
                        onRetry = { reevaluate() },
                        onClose = { finishAffinity() }
                    )
                }
            }
        }
    }

    // Re-contrôle à chaque retour au premier plan : couvre un attachement tardif (Frida, debugger).
    override fun onResume() {
        super.onResume()
        reevaluate()
    }

    private fun reevaluate(): Boolean {
        val assessment = evaluateAccess(applicationContext)
        accessState = assessment
        if (assessment == SecurityAssessment.COMPROMISED_SILENT_EXIT) {
            finishAffinity()
            Process.killProcess(Process.myPid())
            System.exit(1)
            return false
        }
        return true
    }
}
