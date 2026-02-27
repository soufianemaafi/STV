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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
    val topBarColor = MaterialTheme.colorScheme.surface
    val drawerColor = MaterialTheme.colorScheme.surface

    val snackbarHostState = remember { SnackbarHostState() }

    // État pour gérer le debounce du clic
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debounceTime = 1000L // 1 seconde de délai

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = drawerColor
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "STV Player",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.history), color = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.favorites), color = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Privacy Policy Item (Interne - requis par Google Play)
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.privacy_policy), color = MaterialTheme.colorScheme.onSurface) },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Politique de Confidentialité", tint = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, PrivacyPolicyActivity::class.java)
                        context.startActivity(intent)
                        scope.launch { drawerState.close() }
                    }
                )

                // Terms of Service Item (Interne)
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.terms_of_service), color = MaterialTheme.colorScheme.onSurface) },
                    icon = { Icon(Icons.Filled.Description, contentDescription = "Conditions d'Utilisation", tint = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = {
                        val intent = Intent(context, TermsOfServiceActivity::class.java)
                        context.startActivity(intent)
                        scope.launch { drawerState.close() }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quitter
                NavigationDrawerItem(
                    label = { Text("Quitter", color = MaterialTheme.colorScheme.onSurface) },
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = "Quitter", tint = MaterialTheme.colorScheme.onSurface) },
                    selected = false,
                    onClick = {
                        (context as? ComponentActivity)?.finishAffinity()
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
                        Text(
                            text = "STV",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.menu_content_description),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = topBarColor,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = topBarColor.copy(alpha = 0.95f)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        HeroSection()

                        Spacer(modifier = Modifier.height(48.dp))

                        // Bouton + transparent au centre
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime > debounceTime) {
                                    lastClickTime = now
                                    val intent = Intent(context, AddVideoActivity::class.java)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .size(80.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Ajouter un flux",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    QuickActionsSection(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(20.dp),
                        onVideosClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime > debounceTime) {
                                lastClickTime = now
                                val intent = Intent(context, VideoListActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenue dans STV",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Regardez vos vidéos préférées en streaming",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onVideosClick: () -> Unit
) {
    Button(
        onClick = onVideosClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.VideoLibrary,
            contentDescription = "Videos",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.videos_button_label),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
