package com.example.stv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.stv.ui.theme.STVTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installer le SplashScreen avant super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialiser AdMob
        MobileAds.initialize(this) {}

        // Initialiser AdManager et charger l'interstitiel
        adManager = AdManager(this)
        adManager.loadInterstitialAd()

        setContent {
            STVTheme {
                MainScreen(adManager = adManager)
            }
        }
    }

    // Fonction utilitaire pour vérifier la connexion internet
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(adManager: AdManager? = null) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    // État pour gérer le debounce du clic
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debounceTime = 1000L // 1 seconde de délai

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.history)) },
                    selected = false,
                    onClick = { }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.favorites)) },
                    selected = false,
                    onClick = { }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = { }
                )

                // Privacy Policy Item (Requis par Google Play)
                val privacyUrl = stringResource(R.string.privacy_policy_url)
                val termsUrl = stringResource(R.string.terms_of_service_url)
                val uriHandler = LocalUriHandler.current

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.privacy_policy)) },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Politique de Confidentialité") },
                    selected = false,
                    onClick = {
                        try {
                            uriHandler.openUri(privacyUrl)
                        } catch (e: Exception) {
                            // Fallback si aucun navigateur n'est trouvé
                        }
                        scope.launch { drawerState.close() }
                    }
                )

                // Terms of Service Item
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.terms_of_service)) },
                    icon = { Icon(Icons.Filled.Description, contentDescription = "Conditions d'Utilisation") },
                    selected = false,
                    onClick = {
                        try {
                            uriHandler.openUri(termsUrl)
                        } catch (e: Exception) {
                            // Fallback si aucun navigateur n'est trouvé
                        }
                        scope.launch { drawerState.close() }
                    }
                )

            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_logo_stv),
                                contentDescription = "STV Logo",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_content_description))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        ) { contentPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Bouton principal en bas
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > debounceTime) {
                                lastClickTime = now
                                val intent = Intent(context, VideoListActivity::class.java)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VideoLibrary,
                            contentDescription = "Videos",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.videos_button_label),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
