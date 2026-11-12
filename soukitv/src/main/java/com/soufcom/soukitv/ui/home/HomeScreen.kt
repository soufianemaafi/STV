package com.soufcom.soukitv.ui.home

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soufcom.soukitv.R
import com.soufcom.soukitv.features.home.presentation.HomeUiAction
import com.soufcom.soukitv.features.home.presentation.HomeUiState
import com.soufcom.soukitv.features.home.presentation.HomeViewModel
import com.soufcom.soukitv.features.home.presentation.HomeViewModelFactory
import com.soufcom.soukitv.features.home.presentation.NavigationTab
import com.soufcom.soukitv.features.home.presentation.components.ChannelsTab
import com.soufcom.soukitv.features.home.presentation.components.HomeShimmerSkeleton
import com.soufcom.soukitv.features.home.presentation.components.MatchesTab
import com.soufcom.soukitv.features.home.presentation.components.OfflineErrorView
import com.soufcom.soukitv.features.home.presentation.components.SearchAndFilterBar
import com.soufcom.soukitv.features.home.presentation.components.SoukiBottomBar
import com.soufcom.soukitv.features.home.presentation.components.StvErrorDialog
import com.soufcom.soukitv.ui.theme.SoukiDarkSurface
import com.soufcom.soukitv.ui.theme.SoukiRed

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

    val success = currentUiState as? HomeUiState.Success
    val isLoading = currentUiState is HomeUiState.Loading
    val stvPromptType = success?.stvPromptType
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
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = success?.isRefreshing == true,
                onRefresh = { viewModel.onAction(HomeUiAction.RefreshData) },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = success?.isRefreshing == true,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = SoukiDarkSurface,
                        color = SoukiRed
                    )
                }
            ) {
                when {
                    isLoading -> HomeShimmerSkeleton()
                    currentUiState is HomeUiState.Error -> {
                        OfflineErrorView(
                            onRetry = { viewModel.onAction(HomeUiAction.Retry) }
                        )
                    }
                    success != null -> {
                        val loaded = success
                        Column(modifier = Modifier.fillMaxSize()) {
                            val inRootLevel = loaded.activeCategoryDetail == null &&
                                loaded.activeCountryDetail == null
                            if (inRootLevel && loaded.currentTab == NavigationTab.CHANNELS) {
                                SearchAndFilterBar(
                                    query = loaded.searchQuery,
                                    filters = loaded.filterOptions,
                                    selectedFilter = loaded.selectedCategoryFilter,
                                    onQueryChange = {
                                        viewModel.onAction(HomeUiAction.SearchQuery(it))
                                    },
                                    onFilterSelected = {
                                        viewModel.onAction(HomeUiAction.SelectFilter(it))
                                    }
                                )
                            }
                            if (loaded.currentTab == NavigationTab.MATCHES) {
                                MatchesTab(
                                    matches = loaded.visibleMatches,
                                    liveMatchIds = loaded.liveMatchIds,
                                    isFiltering = loaded.searchQuery.isNotBlank() ||
                                        loaded.selectedCategoryFilter != null,
                                    onMatchClick = { match ->
                                        viewModel.onAction(HomeUiAction.SelectMatch(match))
                                    }
                                )
                            } else {
                                ChannelsTab(
                                    categories = loaded.categories,
                                    activeCategory = loaded.activeCategoryDetail,
                                    activeCountry = loaded.activeCountryDetail,
                                    isFlatMode = loaded.isFlatChannelMode,
                                    flatChannels = loaded.visibleChannels,
                                    favorites = loaded.favorites,
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
    }

    if (stvPromptType != null) {
        StvErrorDialog(
            message = null,
            promptType = stvPromptType,
            onDismiss = { viewModel.onAction(HomeUiAction.DismissInstallDialog) },
            onInstall = { viewModel.onAction(HomeUiAction.InstallStv) },
            onRetry = { viewModel.onAction(HomeUiAction.Retry) }
        )
    }
}

