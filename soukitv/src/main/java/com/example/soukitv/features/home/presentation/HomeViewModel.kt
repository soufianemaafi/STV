package com.example.soukitv.features.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.example.soukitv.features.home.data.ChannelRepositoryImpl
import com.example.soukitv.features.home.data.SharedPrefsFavoritesStore
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.isLiveAt
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import com.example.soukitv.features.home.domain.repository.FavoritesStore
import com.example.soukitv.features.home.domain.repository.InMemoryFavoritesStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    private val repository: ChannelRepository,
    private val stvPlayerLauncher: StvPlayerLauncher,
    private val favoritesStore: FavoritesStore = InMemoryFavoritesStore()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var categories: List<Category> = emptyList()
    private var matches: List<MatchItem> = emptyList()
    private var currentTab: NavigationTab = NavigationTab.MATCHES
    private var activeCategoryDetail: Category? = null
    private var activeCountryDetail: String? = null
    private var isDarkTheme: Boolean = true
    private var searchQuery: String = ""
    private var matchFilter: String? = null
    private var channelFilter: String? = null
    private var favorites: Set<String> =
        runCatching { favoritesStore.load() }.getOrDefault(emptySet())

    init {
        loadChannels()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.SelectChannel -> handleSelectChannel(action.channel)
            is HomeUiAction.SelectMatch -> {
                handleLaunchResult(stvPlayerLauncher.launchMatch(action.match))
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
                searchQuery = ""
                activeCategoryDetail = null
                activeCountryDetail = null
                refreshState()
            }
            is HomeUiAction.SelectCategoryDetail -> {
                activeCategoryDetail = action.category
                activeCountryDetail = null
                refreshState()
            }
            is HomeUiAction.SelectCountry -> {
                activeCountryDetail = action.country
                refreshState()
            }
            HomeUiAction.NavigateBack -> {
                if (activeCountryDetail != null) {
                    activeCountryDetail = null
                } else if (activeCategoryDetail != null) {
                    activeCategoryDetail = null
                }
                refreshState()
            }
            is HomeUiAction.SearchQuery -> {
                searchQuery = action.query
                refreshState()
            }
            is HomeUiAction.SelectFilter -> {
                if (currentTab == NavigationTab.MATCHES) {
                    matchFilter = action.category
                } else {
                    channelFilter = action.category
                }
                refreshState()
            }
            is HomeUiAction.ToggleFavorite -> {
                favorites = if (action.channelId in favorites) {
                    favorites - action.channelId
                } else {
                    favorites + action.channelId
                }
                runCatching { favoritesStore.save(favorites) }
                refreshState()
            }
        }
    }

    private fun loadChannels() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Rechargement avec contenu déjà chargé : on garde l'UI et on lève isLoading.
            val current = _uiState.value
            _uiState.value = if (current is HomeUiState.Success) {
                current.copy(isLoading = true)
            } else {
                HomeUiState.Loading
            }
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
                        if (activeCountryDetail != null && activeCategoryDetail == null) {
                            activeCountryDetail = null
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
        handleLaunchResult(stvPlayerLauncher.launchChannel(channel))
    }

    private fun handleLaunchResult(result: LaunchResult) {
        when (result) {
            LaunchResult.Launched -> Unit
            LaunchResult.NotInstalled -> {
                _uiState.value = HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE)
            }
            LaunchResult.UpdateRequired -> {
                _uiState.value = HomeUiState.Error(StvPlayerLauncher.UPDATE_REQUIRED_MESSAGE)
            }
            is LaunchResult.Failed -> {
                _uiState.value = HomeUiState.Error(result.message)
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
        val query = searchQuery.trim()
        val isMatches = currentTab == NavigationTab.MATCHES

        // Un filtre devenu obsolete (catalogue recharge) retombe sur "Tous".
        val matchOptions = matches
            .mapNotNull { it.tournament?.takeIf(String::isNotBlank) }
            .distinct()
        val channelOptions = categories.map { it.name }
        if (matchFilter !in matchOptions) matchFilter = null
        if (channelFilter !in channelOptions) channelFilter = null

        val visibleMatches = matches.filter { match ->
            (matchFilter == null || match.tournament == matchFilter) &&
                (query.isEmpty() ||
                    listOf(match.teamA, match.teamB, match.tournament, match.channelName)
                        .any { it?.contains(query, ignoreCase = true) == true })
        }

        val now = Calendar.getInstance()
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val liveIds = matches.filter { it.isLiveAt(minutes) }.mapNotNull { it.id }.toSet()

        val flat = !isMatches && (query.isNotEmpty() || channelFilter != null)
        val visibleChannels = if (flat) {
            categories
                .filter { channelFilter == null || it.name == channelFilter }
                .flatMap { it.channels }
                .distinctBy { it.id }
                .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
                .sortedByDescending { it.id in favorites }
        } else {
            emptyList()
        }

        _uiState.value = HomeUiState.Success(
            currentTab = currentTab,
            matches = matches,
            categories = categories,
            activeCategoryDetail = activeCategoryDetail,
            activeCountryDetail = activeCountryDetail,
            isDarkTheme = isDarkTheme,
            searchQuery = searchQuery,
            selectedCategoryFilter = if (isMatches) matchFilter else channelFilter,
            filterOptions = if (isMatches) matchOptions else channelOptions,
            visibleMatches = visibleMatches,
            visibleChannels = visibleChannels,
            isFlatChannelMode = flat,
            favorites = favorites,
            liveMatchIds = liveIds
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
                stvPlayerLauncher = StvPlayerLauncher(context.applicationContext),
                favoritesStore = SharedPrefsFavoritesStore(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

