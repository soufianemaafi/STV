package com.example.soukitv.features.home.presentation

import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.toChannel
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import com.example.soukitv.features.home.domain.repository.FavoritesStore
import com.example.soukitv.features.home.domain.repository.InMemoryFavoritesStore
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val launcher: StvPlayerLauncher = mockk(relaxed = true)

    private val news = Category(
        name = "News",
        channels = listOf(
            channel("1", "France 24", "News", "France"),
            channel("2", "Al Jazeera", "News", "Qatar")
        )
    )
    private val sports = Category(
        name = "Sports",
        channels = listOf(
            channel("3", "Red Bull TV", "Sports", "France"),
            channel("4", "Eurosport", "Sports", "Allemagne")
        )
    )
    private val catalogue = listOf(news, sports)
    private val match = matchAt("match-test", "21:00")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        clearMocks(launcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_transitionsToSuccessWithMatchesTabAndCategories() {
        val state = viewModel(matches = listOf(match)).success()

        assertEquals(NavigationTab.MATCHES, state.currentTab)
        assertEquals(catalogue, state.categories)
        assertEquals(listOf(match), state.matches)
        assertNull(state.activeCategoryDetail)
        assertTrue(state.isDarkTheme)
    }

    @Test
    fun matches_areSortedChronologicallyByTime() {
        val late = matchAt("late", "20:00")
        val early = matchAt("early", "18:30")
        val middle = matchAt("middle", "19:15")

        val state = viewModel(matches = listOf(late, early, middle)).success()

        assertEquals(listOf("18:30", "19:15", "20:00"), state.matches.map { it.time })
        assertEquals(listOf("early", "middle", "late"), state.matches.map { it.id })
    }

    @Test
    fun switchTab_togglesBetweenChannelsAndMatches() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        assertEquals(NavigationTab.CHANNELS, viewModel.success().currentTab)

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.MATCHES))
        assertEquals(NavigationTab.MATCHES, viewModel.success().currentTab)
    }

    @Test
    fun switchTab_resetsActiveCategoryDetail() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))
        viewModel.onAction(HomeUiAction.SelectCountry("France"))

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.MATCHES))

        assertNull(viewModel.success().activeCategoryDetail)
        assertNull(viewModel.success().activeCountryDetail)
    }

    @Test
    fun selectCategoryDetail_entersLevelTwoWithSelectedCategory() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))

        viewModel.onAction(HomeUiAction.SelectCategoryDetail(sports))

        assertEquals(sports, viewModel.success().activeCategoryDetail)
        assertEquals(catalogue, viewModel.success().categories)
    }

    @Test
    fun navigateBack_returnsFromLevelTwoToLevelOne() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))

        viewModel.onAction(HomeUiAction.NavigateBack)

        val state = viewModel.success()
        assertNull(state.activeCategoryDetail)
        assertEquals(NavigationTab.CHANNELS, state.currentTab)
    }

    @Test
    fun selectCountry_entersLevelThreeWithSelectedCountry() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))

        viewModel.onAction(HomeUiAction.SelectCountry("France"))

        assertEquals("France", viewModel.success().activeCountryDetail)
        assertEquals(news, viewModel.success().activeCategoryDetail)
    }

    @Test
    fun navigateBack_fromLevelThree_returnsToCountries() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))
        viewModel.onAction(HomeUiAction.SelectCountry("France"))

        viewModel.onAction(HomeUiAction.NavigateBack)

        assertNull(viewModel.success().activeCountryDetail)
        assertEquals(news, viewModel.success().activeCategoryDetail)
    }

    @Test
    fun toggleTheme_invertsDarkThemeState() {
        val viewModel = viewModel()

        assertTrue(viewModel.success().isDarkTheme)

        viewModel.onAction(HomeUiAction.ToggleTheme)
        assertEquals(false, viewModel.success().isDarkTheme)

        viewModel.onAction(HomeUiAction.ToggleTheme)
        assertTrue(viewModel.success().isDarkTheme)
    }

    @Test
    fun selectMatch_launchesStvPlayerWithMatch() {
        every { launcher.launchMatch(any()) } returns StvPlayerLauncher.LaunchResult.Launched
        val viewModel = viewModel(matches = listOf(match))

        viewModel.onAction(HomeUiAction.SelectMatch(match))

        verify { launcher.launchMatch(match) }
    }

    @Test
    fun selectMatch_setsErrorWhenStvIsNotInstalled() {
        every { launcher.launchMatch(any()) } returns StvPlayerLauncher.LaunchResult.NotInstalled
        val viewModel = viewModel(matches = listOf(match))

        viewModel.onAction(HomeUiAction.SelectMatch(match))

        assertEquals(
            HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE),
            viewModel.uiState.value
        )
    }

    @Test
    fun selectChannel_setsErrorWhenStvIsNotInstalled() {
        every { launcher.launchChannel(any()) } returns StvPlayerLauncher.LaunchResult.NotInstalled
        val selectedChannel = sports.channels.first()
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.SelectChannel(selectedChannel))

        assertEquals(
            HomeUiState.Error(StvPlayerLauncher.NOT_INSTALLED_MESSAGE),
            viewModel.uiState.value
        )
    }

    @Test
    fun searchQuery_filtersChannelsByName() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))

        viewModel.onAction(HomeUiAction.SearchQuery("jaz"))

        val state = viewModel.success()
        assertTrue(state.isFlatChannelMode)
        assertEquals(listOf("Al Jazeera"), state.visibleChannels.map { it.name })

        viewModel.onAction(HomeUiAction.SearchQuery(""))
        assertEquals(false, viewModel.success().isFlatChannelMode)
    }

    @Test
    fun selectFilter_filtersChannelsByCategory() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))

        viewModel.onAction(HomeUiAction.SelectFilter("Sports"))

        val state = viewModel.success()
        assertEquals("Sports", state.selectedCategoryFilter)
        assertEquals(sports.channels, state.visibleChannels)

        viewModel.onAction(HomeUiAction.SelectFilter(null))
        assertNull(viewModel.success().selectedCategoryFilter)
        assertEquals(false, viewModel.success().isFlatChannelMode)
    }

    @Test
    fun searchAndFilter_combineToNarrowVisibleChannels() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))

        viewModel.onAction(HomeUiAction.SelectFilter("Sports"))
        viewModel.onAction(HomeUiAction.SearchQuery("euro"))

        val state = viewModel.success()
        assertEquals("euro", state.searchQuery)
        assertEquals(listOf("Eurosport"), state.visibleChannels.map { it.name })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun retry_keepsContentAndEndsWithIsLoadingFalse() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.Retry)

        val state = viewModel.success()
        assertEquals(catalogue, state.categories)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun toggleFavorite_addsAndRemovesIdAndPersistsInStore() {
        val store = InMemoryFavoritesStore()
        val viewModel = viewModel(store = store)

        viewModel.onAction(HomeUiAction.ToggleFavorite("2"))
        assertEquals(setOf("2"), viewModel.success().favorites)
        assertEquals(setOf("2"), store.load())

        viewModel.onAction(HomeUiAction.ToggleFavorite("2"))
        assertTrue(viewModel.success().favorites.isEmpty())
        assertTrue(store.load().isEmpty())
    }

    private fun viewModel(
        categories: List<Category> = catalogue,
        matches: List<MatchItem> = emptyList(),
        store: FavoritesStore = InMemoryFavoritesStore()
    ): HomeViewModel {
        val repository = object : ChannelRepository {
            override fun getCategories(): Flow<List<Category>> = flowOf(categories)
            override fun getMatches(): Flow<List<MatchItem>> = flowOf(matches)
        }
        return HomeViewModel(
            repository = repository,
            stvPlayerLauncher = launcher,
            favoritesStore = store
        )
    }

    private fun HomeViewModel.success(): HomeUiState.Success =
        uiState.value as? HomeUiState.Success
            ?: error("Etat Success attendu, obtenu : ${uiState.value}")

    private fun channel(id: String, name: String, category: String, country: String? = null) = Channel(
        id = id,
        name = name,
        logoUrl = "",
        streamUrl = "https://example.com/$id.m3u8",
        category = category,
        country = country
    )

    private fun matchAt(id: String, time: String) = MatchItem(
        id = id,
        teamA = "Real Madrid",
        teamALogo = "https://example.com/rm.png",
        teamB = "FC Barcelona",
        teamBLogo = "https://example.com/fcb.png",
        time = time,
        tournament = "La Liga",
        channelName = "STV Sports 1",
        streamUrl = "https://example.com/match.m3u8"
    )
}
