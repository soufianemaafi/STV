package com.example.stv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stv.ui.theme.STVTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), adManager: AdManager? = null) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val streamUrl by viewModel.streamUrl.collectAsState()
    val isError by viewModel.isError.collectAsState()

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
                    onClick = { /* TODO */ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.favorites)) },
                    selected = false,
                    onClick = { /* TODO */ }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = { /* TODO */ }
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
                                contentDescription = null,
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = streamUrl,
                        onValueChange = {
                            viewModel.updateUrl(it)
                        },
                        label = { Text(stringResource(R.string.stream_url_label)) },
                        isError = isError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (isError) {
                        Text(
                            text = stringResource(R.string.url_empty_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime < debounceTime) {
                                return@Button // Ignore le clic si trop rapide
                            }
                            lastClickTime = currentTime

                            // Vérification de la connexion internet
                            val isConnected = if (context is MainActivity) {
                                context.isNetworkAvailable()
                            } else {
                                // Fallback basique si le contexte n'est pas l'activité (rare)
                                true
                            }

                            if (!isConnected) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.no_internet_connection))
                                }
                                return@Button
                            }

                            if (viewModel.validateUrl()) {
                                // Fonction pour lancer la vidéo
                                fun startVideo(adsShown: Boolean) {
                                    try {
                                        val intent = Intent(context, PlayerActivity::class.java).apply {
                                            putExtra("VIDEO_URL", streamUrl)
                                            putExtra("SKIP_ADS", adsShown)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.launch_error_prefix, e.message))
                                        }
                                    }
                                }

                                if (adManager != null && context is android.app.Activity) {
                                    // Tenter d'afficher la pub interstitielle
                                    adManager.showInterstitial(
                                        activity = context,
                                        onAdDismissed = {
                                            // Pub vue ou fermée -> on dit au Player de ne pas en remettre
                                            startVideo(true)
                                        },
                                        onFallbackAd = {
                                            // Pas de pub prête ici -> on dit au Player de gérer sa pub
                                            startVideo(false)
                                        }
                                    )
                                } else {
                                    // Fallback si pas de gestionnaire de pub
                                    startVideo(false)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.play_stream_button))
                    }
                }
            }
        }
    }
}
