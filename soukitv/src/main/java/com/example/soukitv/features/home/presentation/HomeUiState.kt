package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.MatchItem

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val allCategories: List<Category>,
        /** null = vue d'ensemble "Tout" */
        val selectedCategoryName: String? = null,
        val searchQuery: String = "",
        /** Liste filtrée selon la recherche et la catégorie active. */
        val displayedCategories: List<Category>,
        /** Matchs du jour, triés par heure croissante. */
        val matches: List<MatchItem> = emptyList(),
        /** Onglet actif de la Bottom Navigation Bar. */
        val currentTab: NavigationTab = NavigationTab.MATCHES,
        /** null = Niveau 1 (liste des catégories) ; non-null = Niveau 2 (chaînes de la catégorie). */
        val activeCategoryDetail: Category? = null,
        /** Thème actif de l'application. */
        val isDarkTheme: Boolean = true
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

