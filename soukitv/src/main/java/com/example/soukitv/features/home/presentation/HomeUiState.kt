package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val allCategories: List<Category>,
        /** null = vue d'ensemble "Tout" */
        val selectedCategoryName: String? = null,
        val searchQuery: String = "",
        /** Liste filtrée selon la recherche et la catégorie active. */
        val displayedCategories: List<Category>
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

