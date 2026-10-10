package com.example.soukitv.features.home.presentation

import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.toChannel
import com.example.soukitv.features.home.domain.repository.ChannelRepository
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
            channel("1", "France 24", "News"),
            channel("2", "Al Jazeera", "News")
        )
    )
    private val sports = Category(
        name = "Sports",
        channels = listOf(
            channel("3", "Red Bull TV", "Sports"),
            channel("4", "Eurosport", "Sports")
        )
    )
    private val catalogue = listOf(news, sports)
    private val match = matchAt("match-test", "21:00")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_showsAllCategoriesWithoutFilters() {
        val state = viewModel().success()

        assertEquals(catalogue, state.allCategories)
        assertNull(state.selectedCategoryName)
        assertEquals("", state.searchQuery)
        assertEquals(catalogue, state.displayedCategories)
    }

    @Test
    fun selectCategory_isolatesThatCategory() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.SelectCategory("Sports"))

        val state = viewModel.success()
        assertEquals("Sports", state.selectedCategoryName)
        assertEquals(listOf(sports), state.displayedCategories)
        assertEquals(catalogue, state.allCategories)
    }

    @Test
    fun selectCategoryNull_showsAllCategoriesAgain() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SelectCategory("News"))

        viewModel.onAction(HomeUiAction.SelectCategory(null))

        val state = viewModel.success()
        assertNull(state.selectedCategoryName)
        assertEquals(catalogue, state.displayedCategories)
    }

    @Test
    fun updateSearchQuery_filtersChannelsByNameIgnoringCase() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.UpdateSearchQuery("EURO"))

        val state = viewModel.success()
        assertEquals("EURO", state.searchQuery)
        assertEquals(1, state.displayedCategories.size)
        assertEquals("Sports", state.displayedCategories.single().name)
        assertEquals(
            listOf("Eurosport"),
            state.displayedCategories.single().channels.map { it.name }
        )
    }

    @Test
    fun updateSearchQuery_withoutMatch_returnsNoCategory() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.UpdateSearchQuery("zzz"))

        assertTrue(viewModel.success().displayedCategories.isEmpty())
    }

    @Test
    fun updateSearchQuery_combinedWithSelectedCategory_restrictsToThatCategory() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SelectCategory("News"))

        viewModel.onAction(HomeUiAction.UpdateSearchQuery("al"))

        val channels = viewModel.success().displayedCategories.single().channels
        assertEquals(listOf("Al Jazeera"), channels.map { it.name })
    }

    @Test
    fun clearFilters_resetsCategoryAndSearch() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SelectCategory("Sports"))
        viewModel.onAction(HomeUiAction.UpdateSearchQuery("red"))

        viewModel.onAction(HomeUiAction.ClearFilters)

        val state = viewModel.success()
        assertNull(state.selectedCategoryName)
        assertEquals("", state.searchQuery)
        assertEquals(catalogue, state.displayedCategories)
    }

    @Test
    fun matches_areExposedInSuccessState() {
        val state = viewModel(matches = listOf(match)).success()

        assertEquals(listOf(match), state.matches)
    }

    @Test
    fun selectMatch_launchesStvPlayerWithMatchAsChannel() {
        val viewModel = viewModel(matches = listOf(match))

        viewModel.onAction(HomeUiAction.SelectMatch(match))

        verify { launcher.launchChannel(match.toChannel()) }
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
    fun defaultTab_isMatches_andNoCategoryDetail() {
        val state = viewModel().success()

        assertEquals(NavigationTab.MATCHES, state.currentTab)
        assertNull(state.activeCategoryDetail)
    }

    @Test
    fun switchTab_togglesCurrentTab() {
        val viewModel = viewModel()

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        assertEquals(NavigationTab.CHANNELS, viewModel.success().currentTab)

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.MATCHES))
        assertEquals(NavigationTab.MATCHES, viewModel.success().currentTab)
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
    fun switchTab_resetsActiveCategoryDetail() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))

        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.MATCHES))

        assertNull(viewModel.success().activeCategoryDetail)
    }

    @Test
    fun selectCategoryDetail_entersCategoryLevelTwo() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))

        viewModel.onAction(HomeUiAction.SelectCategoryDetail(sports))

        assertEquals(sports, viewModel.success().activeCategoryDetail)
    }

    @Test
    fun backToCategoriesList_returnsToLevelOne() {
        val viewModel = viewModel()
        viewModel.onAction(HomeUiAction.SwitchTab(NavigationTab.CHANNELS))
        viewModel.onAction(HomeUiAction.SelectCategoryDetail(news))

        viewModel.onAction(HomeUiAction.BackToCategoriesList)

        val state = viewModel.success()
        assertNull(state.activeCategoryDetail)
        assertEquals(NavigationTab.CHANNELS, state.currentTab)
    }

    private fun viewModel(
        categories: List<Category> = catalogue,
        matches: List<MatchItem> = emptyList()
    ): HomeViewModel {
        val repository = object : ChannelRepository {
            override fun getCategories(): Flow<List<Category>> = flowOf(categories)
            override fun getMatches(): Flow<List<MatchItem>> = flowOf(matches)
        }
        return HomeViewModel(repository = repository, stvPlayerLauncher = launcher)
    }

    private fun HomeViewModel.success(): HomeUiState.Success =
        uiState.value as? HomeUiState.Success
            ?: error("Etat Success attendu, obtenu : ${uiState.value}")

    private fun channel(id: String, name: String, category: String) = Channel(
        id = id,
        name = name,
        logoUrl = "",
        streamUrl = "https://example.com/$id.m3u8",
        category = category
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

