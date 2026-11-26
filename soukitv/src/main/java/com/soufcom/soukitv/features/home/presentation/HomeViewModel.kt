package com.soufcom.soukitv.features.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.soufcom.soukitv.core.player.StvPlayerLauncher
import com.soufcom.soukitv.core.security.defaultTrustedHosts
import com.soufcom.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.soufcom.soukitv.features.home.data.ChannelRepositoryImpl
import com.soufcom.soukitv.features.home.data.SharedPrefsFavoritesStore
import com.soufcom.soukitv.features.home.domain.model.Category
import com.soufcom.soukitv.features.home.domain.model.Channel
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.isLiveAt
import com.soufcom.soukitv.features.home.domain.model.parseStartMinutes
import com.soufcom.soukitv.features.home.domain.repository.ChannelRepository
import com.soufcom.soukitv.features.home.domain.repository.FavoritesStore
import com.soufcom.soukitv.features.home.domain.repository.InMemoryFavoritesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    private val favoritesStore: FavoritesStore = InMemoryFavoritesStore(),
    private val launchDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var categories: List<Category> = emptyList()
    private var matches: List<MatchItem> = emptyList()
    private var currentTab: NavigationTab = NavigationTab.MATCHES
    private var activeCategoryDetail: Category? = null
    private var activeCountryDetail: String? = null
    private var currentCategoryPath: String? = null
    private var isDarkTheme: Boolean = true
    private var searchQuery: String = ""
    private var launchErrorEventId: Long? = null
    private var launchErrorCounter: Long = 0L
    private var matchFilter: String? = null
    private var channelFilter: String? = null
    private var favorites: Set<String> =
        runCatching { favoritesStore.load() }.getOrDefault(emptySet())
    private var minStvVersionCode: Long = StvPlayerLauncher.MIN_STV_VERSION_CODE
    private var authSecret: String = ""
    private var trustedHosts: Set<String> = defaultTrustedHosts()

    init {
        loadChannels()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.SelectChannel -> handleSelectChannel(action.channel)
            is HomeUiAction.SelectMatch -> {
                handleSelectMatch(action.match)
            }
            HomeUiAction.DismissInstallDialog -> restoreSuccessState()
            HomeUiAction.DismissLaunchError -> {
                launchErrorEventId = null
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    _uiState.value = current.copy(launchErrorEventId = null)
                }
            }
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
                currentCategoryPath = null
                refreshState()
            }
            is HomeUiAction.SelectCategoryDetail -> {
                activeCategoryDetail = action.category
                activeCountryDetail = null
                currentCategoryPath = action.category.name
                refreshState()
            }
            is HomeUiAction.SelectCountry -> {
                activeCountryDetail = action.country
                refreshState()
            }
            HomeUiAction.NavigateBack -> {
                if (activeCountryDetail != null) {
                    activeCountryDetail = null
                } else if (currentCategoryPath?.contains('/') == true) {
                    currentCategoryPath = currentCategoryPath?.substringBeforeLast('/')
                    activeCategoryDetail = findCategory(currentCategoryPath)
                } else if (currentCategoryPath != null) {
                    currentCategoryPath = null
                    activeCategoryDetail = null
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
            HomeUiAction.RefreshData -> loadChannels(isRefresh = true)
        }
    }

    private fun loadChannels(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Rechargement avec contenu déjà chargé : on garde l'UI intacte et on lève isRefreshing ou isLoading.
            val current = _uiState.value
            _uiState.value = if (isRefresh && current is HomeUiState.Success) {
                current.copy(isRefreshing = true)
            } else if (current is HomeUiState.Success) {
                current.copy(isLoading = true)
            } else {
                HomeUiState.Loading
            }
            runCatching {
                repository.getCategories()
                    .combine(repository.getMatches()) { categories, todayMatches ->
                        categories to todayMatches
                    }
                    .combine(repository.getRemoteConfig()) { (categories, todayMatches), remoteConfig ->
                        Triple(categories, todayMatches, remoteConfig)
                    }
                    .collect { (categories, todayMatches, remoteConfig) ->
                        val validatedAuthSecret = remoteConfig.authSecret
                            .trim()
                            .takeIf(String::isNotEmpty)
                            ?: throw SecurityException("Remote security configuration unavailable")
                        this@HomeViewModel.categories = categories
                        // Tri chronologique numérique ; heures absentes ou invalides en fin de liste.
                        matches = todayMatches.sortedBy { it.parseStartMinutes() ?: Int.MAX_VALUE }
                        minStvVersionCode = remoteConfig.minVersionCode
                        authSecret = validatedAuthSecret
                        trustedHosts = remoteConfig.trustedHosts.ifEmpty { defaultTrustedHosts() }
                        currentCategoryPath = currentCategoryPath?.takeIf { path ->
                            categoryPaths().contains(path)
                        }
                        // Re-résout le chemin ouvert sur le nouveau catalogue.
                        activeCategoryDetail = currentCategoryPath?.let(::findCategory)
                        if (activeCountryDetail != null && activeCategoryDetail == null) {
                            activeCountryDetail = null
                        }
                        publishSuccess(isRefreshing = false)
                    }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                val isSecurityFailure = throwable is SecurityException ||
                    throwable.cause is SecurityException
                if (isSecurityFailure) {
                    _uiState.value = HomeUiState.Error(
                        StvPlayerLauncher.SECURITY_CONFIGURATION_ERROR_MESSAGE
                    )
                    return@onFailure
                }
                val prevSuccess = _uiState.value as? HomeUiState.Success
                if (prevSuccess != null) {
                    _uiState.value = prevSuccess.copy(isRefreshing = false, isLoading = false)
                } else {
                    _uiState.value = HomeUiState.Error(
                        "Connexion Internet indisponible. Veuillez vérifier votre réseau."
                    )
                }
            }
        }
    }

    private fun handleSelectChannel(channel: Channel) {
        viewModelScope.launch(launchDispatcher) {
            handleLaunchResult(
                stvPlayerLauncher.launchChannel(
                    channel = channel,
                    minStvVersionCode = minStvVersionCode,
                    authSecret = authSecret,
                    trustedHosts = trustedHosts
                )
            )
        }
    }

    private fun handleSelectMatch(match: MatchItem) {
        viewModelScope.launch(launchDispatcher) {
            handleLaunchResult(
                stvPlayerLauncher.launchMatch(
                    match = match,
                    minStvVersionCode = minStvVersionCode,
                    authSecret = authSecret,
                    trustedHosts = trustedHosts
                )
            )
        }
    }

    private fun handleLaunchResult(result: LaunchResult) {
        when (result) {
            LaunchResult.Launched -> Unit
            LaunchResult.NotInstalled -> {
                showStvPrompt(StvPromptType.Install)
            }
            LaunchResult.UpdateRequired -> {
                showStvPrompt(StvPromptType.Update)
            }
            is LaunchResult.Failed -> {
                val current = _uiState.value
                if (current is HomeUiState.Success) {
                    launchErrorCounter += 1
                    launchErrorEventId = launchErrorCounter
                    _uiState.value = current.copy(launchErrorEventId = launchErrorEventId)
                } else {
                    // [B3] Ne jamais exposer result.message en production :
                    // e.message peut contenir des noms de classes Android internes
                    // (ActivityNotFoundException, etc.) visibles par instrumentation.
                    _uiState.value = HomeUiState.Error(
                        "Impossible de lancer la lecture. Veuillez réessayer."
                    )
                }
            }
        }
    }

    private fun showStvPrompt(promptType: StvPromptType) {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(stvPromptType = promptType)
            return
        }
        _uiState.value = HomeUiState.Error(
            when (promptType) {
                StvPromptType.Install -> StvPlayerLauncher.NOT_INSTALLED_MESSAGE
                StvPromptType.Update -> StvPlayerLauncher.UPDATE_REQUIRED_MESSAGE
            }
        )
    }

    private fun restoreSuccessState() {
        if (categories.isNotEmpty()) {
            publishSuccess()
        } else {
            loadChannels()
        }
    }

    /** Republie l'etat uniquement si le catalogue est affiche. */
    private fun refreshState() {
        val current = _uiState.value as? HomeUiState.Success ?: return
        publishSuccess(isRefreshing = current.isRefreshing)
    }

    private fun publishSuccess(isRefreshing: Boolean = false) {
        val query = searchQuery.trim()
        val isMatches = currentTab == NavigationTab.MATCHES

        // Un filtre devenu obsolete (catalogue recharge) retombe sur "Tous".
        val matchOptions = matches
            .mapNotNull { it.tournament?.takeIf(String::isNotBlank) }
            .distinct()
        val channelOptions = rootCategoryPaths().filter { path ->
            categoryForPath(path)?.channelCount?.let { it > 0 } == true
        }
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

        val currentPath = currentCategoryPath
        val currentHasChildren = currentPath?.let(::hasChildren) == true
        val flat = !isMatches && (
            query.isNotEmpty() ||
                channelFilter != null ||
                (currentPath != null && !currentHasChildren)
            )
        val visibleChannels = if (flat) {
            val allChannels = allCatalogChannels()
            allChannels
                .filter { channel ->
                    val filterMatches = channelFilter == null || channel.categories.any {
                        it == channelFilter || it.startsWith("$channelFilter/")
                    }
                    val pathMatches = currentPath == null ||
                        channel.categories.any { it == currentPath }
                    val queryMatches = query.isEmpty() || listOf(
                        channel.name,
                        channel.primaryCategory,
                        channel.categories.joinToString(" ")
                    ).any { it.contains(query, ignoreCase = true) }
                    filterMatches && pathMatches && queryMatches
                }
                .sortedByDescending { it.id in favorites }
        } else {
            emptyList()
        }

        val visibleCategories = when {
            isMatches -> emptyList()
            currentPath == null -> rootCategoryPaths().mapNotNull(::categoryForPath)
            currentHasChildren -> categoryPaths()
                .filter { it.substringBeforeLast('/') == currentPath && it != currentPath }
                .mapNotNull(::categoryForPath)
            else -> emptyList()
        }.filter { it.channelCount > 0 }

        _uiState.value = HomeUiState.Success(
            currentTab = currentTab,
            matches = matches,
            categories = categories,
            currentCategoryPath = currentPath,
            visibleCategories = visibleCategories,
            activeCategoryDetail = activeCategoryDetail,
            activeCountryDetail = activeCountryDetail,
            isDarkTheme = isDarkTheme,
            searchQuery = searchQuery,
            selectedCategoryFilter = if (isMatches) matchFilter else channelFilter,
            filterOptions = if (isMatches) matchOptions else channelOptions,
            visibleMatches = visibleMatches,
            visibleChannels = visibleChannels,
            isFlatChannelMode = flat,
            launchErrorEventId = launchErrorEventId,
            favorites = favorites,
            liveMatchIds = liveIds,
            isRefreshing = isRefreshing
        )
    }

    private fun allCatalogChannels(): List<Channel> =
        categories.flatMap { it.channels }.distinctBy { it.id }

    private fun categoryPaths(): List<String> {
        val paths = LinkedHashSet<String>()
        categories.forEach { category ->
            val parts = category.name.split('/').filter(String::isNotBlank)
            parts.indices.forEach { index ->
                paths += parts.take(index + 1).joinToString("/")
            }
        }
        return paths.toList()
    }

    private fun rootCategoryPaths(): List<String> =
        categoryPaths().filterNot { '/' in it }

    private fun hasChildren(path: String): Boolean =
        categoryPaths().any { it.startsWith("$path/") }

    private fun categoryForPath(path: String): Category? {
        val declared = categories.firstOrNull { it.name == path }
        val channels = allCatalogChannels().filter { channel ->
            channel.categories.any { it == path || it.startsWith("$path/") }
        }
        return if (channels.isEmpty()) {
            null
        } else {
            Category(
                name = path,
                channels = channels,
                imageUrl = declared?.imageUrl
            )
        }
    }

    private fun findCategory(path: String?): Category? = path?.let(::categoryForPath)

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

