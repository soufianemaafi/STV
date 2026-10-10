package com.example.soukitv.features.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.example.soukitv.features.home.data.ChannelRepositoryImpl
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ChannelRepository,
    private val stvPlayerLauncher: StvPlayerLauncher
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var categories: List<Category> = emptyList()
    private var matches: List<MatchItem> = emptyList()
    private var currentTab: NavigationTab = NavigationTab.MATCHES
    private var activeCategoryDetail: Category? = null
    private var isDarkTheme: Boolean = true

    init {
        loadChannels()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.SelectChannel -> handleSelectChannel(action.channel)
            is HomeUiAction.SelectMatch -> {
                val matchChannel = Channel(
                    id = action.match.id,
                    name = "${action.match.teamA} vs ${action.match.teamB}",
                    logoUrl = action.match.teamALogo,
                    streamUrl = action.match.streamUrl,
                    category = action.match.tournament
                )
                val result = stvPlayerLauncher.launchChannel(matchChannel)
                if (result is LaunchResult.NotInstalled) {
                    _uiState.value = HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE)
                }
            }
            HomeUiAction.DismissInstallDialog -> restoreSuccessState()
            HomeUiAction.InstallStv -> {
                stvPlayerLauncher.openPlayStore()
                restoreSuccessState()
            }
            HomeUiAction.Retry -> loadChannels()
            HomeUiAction.ToggleTheme -> {
                isDarkTheme = !isDarkTheme
                refreshState()
            }
            is HomeUiAction.SwitchTab -> {
                currentTab = action.tab
                activeCategoryDetail = null
                refreshState()
            }
            is HomeUiAction.SelectCategoryDetail -> {
                activeCategoryDetail = action.category
                refreshState()
            }
            HomeUiAction.BackToCategoriesList -> {
                activeCategoryDetail = null
                refreshState()
            }
        }
    }

    private fun loadChannels() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            runCatching {
                repository.getCategories()
                    .combine(repository.getMatches()) { categories, todayMatches ->
                        categories to todayMatches
                    }
                    .collect { (categories, todayMatches) ->
                        this@HomeViewModel.categories = categories
                        // Tri chronologique des matchs (format HH:mm).
                        matches = todayMatches.sortedBy { it.time }
                        // Re-resout la categorie ouverte sur le nouveau catalogue.
                        activeCategoryDetail = activeCategoryDetail?.let { open ->
                            categories.firstOrNull { it.name == open.name }
                        }
                        publishSuccess()
                    }
            }.onFailure { throwable ->
                _uiState.value = HomeUiState.Error(
                    throwable.message ?: "Unable to load channels"
                )
            }
        }
    }

    private fun handleSelectChannel(channel: Channel) {
        when (val result = stvPlayerLauncher.launchChannel(channel)) {
            LaunchResult.Launched -> Unit
            LaunchResult.NotInstalled -> {
                _uiState.value = HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE)
            }
            is LaunchResult.Failed -> {
                _uiState.value = HomeUiState.Error(
                    result.message
                )
            }
        }
    }

    private fun restoreSuccessState() {
        if (categories.isNotEmpty()) {
            publishSuccess()
        } else {
            _uiState.value = HomeUiState.Loading
        }
    }

    /** Republie l'etat uniquement si le catalogue est affiche. */
    private fun refreshState() {
        if (_uiState.value is HomeUiState.Success) publishSuccess()
    }

    private fun publishSuccess() {
        _uiState.value = HomeUiState.Success(
            currentTab = currentTab,
            matches = matches,
            categories = categories,
            activeCategoryDetail = activeCategoryDetail,
            isDarkTheme = isDarkTheme
        )
    }
}

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                repository = ChannelRepositoryImpl(),
                stvPlayerLauncher = StvPlayerLauncher(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

