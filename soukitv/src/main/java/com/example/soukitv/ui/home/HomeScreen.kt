package com.example.soukitv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soukitv.R
import com.example.soukitv.features.home.presentation.HomeUiAction
import com.example.soukitv.features.home.presentation.HomeUiState
import com.example.soukitv.features.home.presentation.HomeViewModel
import com.example.soukitv.features.home.presentation.HomeViewModelFactory
import com.example.soukitv.features.home.presentation.NavigationTab
import com.example.soukitv.features.home.presentation.components.ChannelsTab
import com.example.soukitv.features.home.presentation.components.HomeShimmerSkeleton
import com.example.soukitv.features.home.presentation.components.MatchesTab
import com.example.soukitv.features.home.presentation.components.SearchAndFilterBar
import com.example.soukitv.features.home.presentation.components.SoukiBottomBar
import com.example.soukitv.features.home.presentation.components.StvErrorDialog
import com.example.soukitv.ui.theme.SoukiDarkSurface
import com.example.soukitv.ui.theme.SoukiRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext)
    )
) {
    val currentUiState by viewModel.uiState.collectAsState()
    val activeDetail = (currentUiState as? HomeUiState.Success)?.activeCategoryDetail
    val activeCountry = (currentUiState as? HomeUiState.Success)?.activeCountryDetail
    val activeTab = (currentUiState as? HomeUiState.Success)?.currentTab

    BackHandler(enabled = activeDetail != null || activeCountry != null) {
        viewModel.onAction(HomeUiAction.NavigateBack)
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
    val stvPromptType = (currentUiState as? HomeUiState.Success)?.stvPromptType
    val selectedTab = success?.currentTab ?: NavigationTab.MATCHES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    val isDarkTheme = success?.isDarkTheme ?: true
                    IconButton(
                        onClick = { viewModel.onAction(HomeUiAction.ToggleTheme) }
                    ) {
                        Crossfade(targetState = isDarkTheme, label = "theme_icon") { darkTheme ->
                            Icon(
                                imageVector = if (darkTheme) {
                                    Icons.Default.LightMode
                                } else {
                                    Icons.Default.DarkMode
                                },
                                contentDescription = stringResource(
                                    if (darkTheme) {
                                        R.string.theme_light_mode
                                    } else {
                                        R.string.theme_dark_mode
                                    }
                                ),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
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
            if (success == null) {
                if (errorState == null) HomeShimmerSkeleton()
            } else {
                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = success.isRefreshing,
                    onRefresh = { viewModel.onAction(HomeUiAction.RefreshData) },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = success.isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = SoukiDarkSurface,
                            color = SoukiRed
                        )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val inRootLevel = success.activeCategoryDetail == null &&
                            success.activeCountryDetail == null
                        if (inRootLevel && success.currentTab == NavigationTab.CHANNELS) {
                            SearchAndFilterBar(
                                query = success.searchQuery,
                                filters = success.filterOptions,
                                selectedFilter = success.selectedCategoryFilter,
                                onQueryChange = { viewModel.onAction(HomeUiAction.SearchQuery(it)) },
                                onFilterSelected = { viewModel.onAction(HomeUiAction.SelectFilter(it)) }
                            )
                        }
                        if (success.currentTab == NavigationTab.MATCHES) {
                            MatchesTab(
                                matches = success.visibleMatches,
                                liveMatchIds = success.liveMatchIds,
                                isFiltering = success.searchQuery.isNotBlank() ||
                                    success.selectedCategoryFilter != null,
                                onMatchClick = { match ->
                                    viewModel.onAction(HomeUiAction.SelectMatch(match))
                                }
                            )
                        } else {
                            ChannelsTab(
                                categories = success.categories,
                                activeCategory = success.activeCategoryDetail,
                                activeCountry = success.activeCountryDetail,
                                isFlatMode = success.isFlatChannelMode,
                                flatChannels = success.visibleChannels,
                                favorites = success.favorites,
                                onCategoryClick = { category ->
                                    viewModel.onAction(HomeUiAction.SelectCategoryDetail(category))
                                },
                                onCountryClick = { country ->
                                    viewModel.onAction(HomeUiAction.SelectCountry(country))
                                },
                                onBackClick = {
                                    viewModel.onAction(HomeUiAction.NavigateBack)
                                },
                                onChannelClick = { channel ->
                                    viewModel.onAction(HomeUiAction.SelectChannel(channel))
                                },
                                onToggleFavorite = { channel ->
                                    viewModel.onAction(HomeUiAction.ToggleFavorite(channel.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (errorState != null || stvPromptType != null) {
        StvErrorDialog(
            message = errorState?.message,
            promptType = stvPromptType,
            onDismiss = { viewModel.onAction(HomeUiAction.DismissInstallDialog) },
            onInstall = { viewModel.onAction(HomeUiAction.InstallStv) },
            onRetry = { viewModel.onAction(HomeUiAction.Retry) }
        )
    }
}

