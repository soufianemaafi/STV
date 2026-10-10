package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Channel

sealed interface HomeUiAction {
    data class SelectChannel(val channel: Channel) : HomeUiAction
    data object DismissInstallDialog : HomeUiAction
    data object InstallStv : HomeUiAction
    data object Retry : HomeUiAction
    /** null = afficher toutes les catégories. */
    data class SelectCategory(val categoryName: String?) : HomeUiAction
    data class UpdateSearchQuery(val query: String) : HomeUiAction
    data object ClearFilters : HomeUiAction
}

