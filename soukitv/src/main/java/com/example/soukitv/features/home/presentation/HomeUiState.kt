package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.MatchItem

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val currentTab: NavigationTab = NavigationTab.MATCHES,
        val matches: List<MatchItem> = emptyList(),
        val categories: List<Category> = emptyList(),
        val activeCategoryDetail: Category? = null,
        val isDarkTheme: Boolean = true
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

