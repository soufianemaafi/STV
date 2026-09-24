package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val categories: List<Category>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

