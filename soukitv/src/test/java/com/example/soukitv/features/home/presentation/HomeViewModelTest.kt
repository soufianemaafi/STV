package com.example.soukitv.features.home.presentation

import com.example.soukitv.core.player.StvPlayerLauncher
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import io.mockk.mockk
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

    private fun viewModel(categories: List<Category> = catalogue): HomeViewModel {
        val repository = object : ChannelRepository {
            override fun getCategories(): Flow<List<Category>> = flowOf(categories)
        }
        return HomeViewModel(repository = repository, stvPlayerLauncher = launcher)
    }

    private fun HomeViewModel.success(): HomeUiState.Success =
        uiState.value as? HomeUiState.Success
            ?: error("État Success attendu, obtenu : ${uiState.value}")

    private fun channel(id: String, name: String, category: String) = Channel(
        id = id,
        name = name,
        logoUrl = "",
        streamUrl = "https://example.com/$id.m3u8",
        category = category
    )
}

