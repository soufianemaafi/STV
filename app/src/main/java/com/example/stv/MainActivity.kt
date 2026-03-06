package com.example.stv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.stv.ui.theme.STVTheme
import com.example.stv.ui.theme.BlackDrawer
import com.example.stv.ui.theme.WhitePrimary
import com.example.stv.ui.theme.GrayLight
import com.example.stv.ui.theme.GrayMuted
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adManager: AdManager
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installer le SplashScreen avant super.onCreate()
        val splashScreen = installSplashScreen()


        // Garder le splash screen visible pendant au moins 1 seconde
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        super.onCreate(savedInstanceState)

        // Initialiser AdMob
        MobileAds.initialize(this) {}

        // Initialiser AdManager et charger l'interstitiel
        adManager = AdManager(this)
        adManager.loadInterstitialAd()

        setContent {
            STVTheme {
                MainScreen(_adManager = adManager)
            }
        }

        // Retirer le splash screen après 500ms (animation standard)
        window.decorView.postDelayed({
            keepSplashScreen = false
        }, 500)
    }

    // Fonction utilitaire pour vérifier la connexion internet
    // ✅ Modernisé : utilise NetworkCapabilities au lieu de l'API dépréciée activeNetworkInfo
    @Suppress("UNUSED")  // À utiliser dans v1.1 pour vérifier connexion avant streaming
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Depuis minSdk=24, on utilise NetworkCapabilities directement
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Vérifie que la connexion Internet est présente ET validée
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(@Suppress("UNUSED_PARAMETER") _adManager: AdManager? = null) {
    // Reçu de MainActivity.onCreate() - À utiliser dans v1.1 pour afficher les pubs
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
            ModalDrawerSheet(
                modifier = Modifier
                    .width(280.dp)
                    .windowInsetsPadding(WindowInsets.systemBars),
                drawerContainerColor = BlackDrawer,
                drawerShape = RectangleShape
            ) {
                // Logo/Titre du haut
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "STV",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = WhitePrimary
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )

                // Section 1 : Contenu principal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.history), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.Menu, contentDescription = null, tint = GrayLight) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.favorites), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = GrayLight) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.settings), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = GrayLight) },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }

                HorizontalDivider(
                    color = GrayMuted,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Section 2 : Informations légales
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.privacy_policy), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.privacy_policy_cd), tint = GrayLight) },
                        selected = false,
                        onClick = {
                            val intent = Intent(context, PrivacyPolicyActivity::class.java)
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.terms_of_service), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.Description, contentDescription = "Terms of Service", tint = GrayLight) },
                        selected = false,
                        onClick = {
                            val intent = Intent(context, TermsOfServiceActivity::class.java)
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }

                HorizontalDivider(
                    color = GrayMuted,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Section 3 : Quitter
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_quit), color = WhitePrimary) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.menu_quit), tint = GrayLight) },
                    selected = false,
                    onClick = {
                        (context as? ComponentActivity)?.finishAffinity()
                    },
                    colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
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
                            color = MaterialTheme.colorScheme.onPrimary
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
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
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
                                contentDescription = "Add stream",
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
            text = stringResource(R.string.welcome_message),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.tagline_message),
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

