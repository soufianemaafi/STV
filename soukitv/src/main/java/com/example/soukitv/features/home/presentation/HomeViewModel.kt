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
    private var allCategories: List<Category> = emptyList()
    private var matches: List<MatchItem> = emptyList()
    private var selectedCategoryName: String? = null
    private var searchQuery: String = ""
    private var currentTab: NavigationTab = NavigationTab.MATCHES
    private var activeCategoryDetail: Category? = null

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
            is HomeUiAction.SwitchTab -> {
                currentTab = action.tab
                activeCategoryDetail = null
                refreshFilters()
            }
            is HomeUiAction.SelectCategoryDetail -> {
                activeCategoryDetail = action.category
                refreshFilters()
            }
            HomeUiAction.BackToCategoriesList -> {
                activeCategoryDetail = null
                refreshFilters()
            }
            is HomeUiAction.SelectCategory -> {
                selectedCategoryName = action.categoryName
                refreshFilters()
            }
            is HomeUiAction.UpdateSearchQuery -> {
                searchQuery = action.query
                refreshFilters()
            }
            HomeUiAction.ClearFilters -> {
                selectedCategoryName = null
                searchQuery = ""
                refreshFilters()
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
                        allCategories = categories
                        // Tri chronologique des matchs (format HH:mm).
                        matches = todayMatches.sortedBy { it.time }
                        // Re-resout la categorie ouverte sur le nouveau catalogue.
                        activeCategoryDetail = activeCategoryDetail?.let { open ->
                            categories.firstOrNull { it.name == open.name }
                        }
                        if (categories.none { it.name == selectedCategoryName }) {
                            selectedCategoryName = null
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
        if (allCategories.isNotEmpty()) {
            publishSuccess()
        } else {
            _uiState.value = HomeUiState.Loading
        }
    }

    /** Recalcule les filtres uniquement si le catalogue est affiche (evite d'ecraser Loading/Error). */
    private fun refreshFilters() {
        if (_uiState.value is HomeUiState.Success) publishSuccess()
    }

    private fun publishSuccess() {
        _uiState.value = HomeUiState.Success(
            allCategories = allCategories,
            selectedCategoryName = selectedCategoryName,
            searchQuery = searchQuery,
            displayedCategories = computeDisplayedCategories(
                allCategories = allCategories,
                selectedCategoryName = selectedCategoryName,
                query = searchQuery
            ),
            matches = matches,
            currentTab = currentTab,
            activeCategoryDetail = activeCategoryDetail
        )
    }

    companion object {
        /**
         * Filtre pur : restriction a la categorie selectionnee (si presente),
         * puis recherche insensible a la casse sur le nom des chaines (si non vide).
         */
        internal fun computeDisplayedCategories(
            allCategories: List<Category>,
            selectedCategoryName: String?,
            query: String
        ): List<Category> {
            val normalizedQuery = query.trim()
            return allCategories
                .asSequence()
                .filter { selectedCategoryName == null || it.name == selectedCategoryName }
                .map { category ->
                    if (normalizedQuery.isEmpty()) {
                        category
                    } else {
                        category.copy(
                            channels = category.channels.filter {
                                it.name.contains(normalizedQuery, ignoreCase = true)
                            }
                        )
                    }
                }
                .filter { normalizedQuery.isEmpty() || it.channels.isNotEmpty() }
                .toList()
        }
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

