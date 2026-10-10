package com.example.soukitv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soukitv.R
import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.features.home.presentation.HomeUiAction
import com.example.soukitv.features.home.presentation.HomeUiState
import com.example.soukitv.features.home.presentation.HomeViewModel
import com.example.soukitv.features.home.presentation.HomeViewModelFactory
import com.example.soukitv.features.home.presentation.NavigationTab
import com.example.soukitv.features.home.presentation.components.ChannelsTab
import com.example.soukitv.features.home.presentation.components.MatchesTab
import com.example.soukitv.features.home.presentation.components.StvErrorDialog
import com.example.soukitv.features.home.presentation.components.SoukiBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext)
    )
) {
    val currentUiState by viewModel.uiState.collectAsState()
    val activeDetail = (currentUiState as? HomeUiState.Success)?.activeCategoryDetail
    val activeTab = (currentUiState as? HomeUiState.Success)?.currentTab

    BackHandler(enabled = activeDetail != null) {
        viewModel.onAction(HomeUiAction.BackToCategoriesList)
    }

    BackHandler(enabled = activeTab == NavigationTab.CHANNELS && activeDetail == null) {
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.MATCHES))
    }

    var lastSuccess by remember { mutableStateOf<HomeUiState.Success?>(null) }
    LaunchedEffect(currentUiState) {
        val newSuccess = currentUiState as? HomeUiState.Success
        if (newSuccess != null) {
            lastSuccess = newSuccess
        }
    }

    val success = (currentUiState as? HomeUiState.Success) ?: lastSuccess
    val errorState = currentUiState as? HomeUiState.Error
    val selectedTab = success?.currentTab ?: NavigationTab.MATCHES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            SoukiBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    viewModel.onAction(HomeUiAction.SwitchTab(tab))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                success == null && errorState == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                success?.currentTab == NavigationTab.MATCHES -> {
                    MatchesTab(
                        matches = success.matches,
                        onMatchClick = { match ->
                            viewModel.onAction(HomeUiAction.SelectMatch(match))
                        }
                    )
                }

                success != null -> {
                    ChannelsTab(
                        categories = success.allCategories,
                        activeCategory = success.activeCategoryDetail,
                        onCategoryClick = { category ->
                            viewModel.onAction(HomeUiAction.SelectCategoryDetail(category))
                        },
                        onBackClick = {
                            viewModel.onAction(HomeUiAction.BackToCategoriesList)
                        },
                        onChannelClick = { channel ->
                            viewModel.onAction(HomeUiAction.SelectChannel(channel))
                        }
                    )
                }
            }
        }
    }

    if (errorState != null) {
        StvErrorDialog(
            message = errorState.message,
            installRequired = errorState.message == StvPlayerLauncher.NOT_INSTALLED_MESSAGE,
            onDismiss = { viewModel.onAction(HomeUiAction.DismissInstallDialog) },
            onInstall = { viewModel.onAction(HomeUiAction.InstallStv) },
            onRetry = { viewModel.onAction(HomeUiAction.Retry) }
        )
    }
}
