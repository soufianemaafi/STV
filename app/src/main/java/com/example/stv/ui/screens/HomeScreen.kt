package com.example.stv.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stv.R
import com.example.stv.ui.theme.BlackDrawer
import com.example.stv.ui.theme.GrayLight
import com.example.stv.ui.theme.GrayMuted
import com.example.stv.ui.theme.WhitePrimary
import com.example.stv.util.NetworkUtils
import kotlinx.coroutines.launch

/**
 * Écran d'accueil — anciennement MainScreen() dans MainActivity.kt
 *
 * @param onAddVideoClick  Naviguer vers l'écran AddVideo
 * @param onVideosClick    Naviguer vers l'écran VideoList
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddVideoClick: () -> Unit,
    onVideosClick: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val noInternetMessage = stringResource(R.string.no_internet_connection)
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    val termsOfServiceUrl = stringResource(R.string.terms_of_service_url)

    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debounceTime = 1000L

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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.terms_of_service), color = WhitePrimary) },
                        icon = { Icon(Icons.Filled.Description, contentDescription = stringResource(R.string.terms_of_service_cd), tint = GrayLight) },
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(termsOfServiceUrl))
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }

                HorizontalDivider(
                    color = GrayMuted,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_quit), color = WhitePrimary) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.menu_quit), tint = GrayLight) },
                    selected = false,
                    onClick = {
                        (context as? ComponentActivity)?.finishAffinity()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
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
                            scope.launch { drawerState.open() }
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

                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime > debounceTime) {
                                    lastClickTime = now
                                    if (NetworkUtils.isNetworkAvailable(context)) {
                                        onAddVideoClick()
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(noInternetMessage)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_stream_button),
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
                                if (NetworkUtils.isNetworkAvailable(context)) {
                                    onVideosClick()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(noInternetMessage)
                                    }
                                }
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
            contentDescription = stringResource(R.string.videos_button_label),
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

