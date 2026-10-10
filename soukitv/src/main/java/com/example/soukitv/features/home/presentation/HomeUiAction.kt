package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem

/** Onglets de la Bottom Navigation Bar. */
enum class NavigationTab { MATCHES, CHANNELS }

sealed interface HomeUiAction {
    data class SwitchTab(val tab: NavigationTab) : HomeUiAction
    /** Niveau 2 : ouvre la grille des chaînes d'une catégorie. */
    data class SelectCategoryDetail(val category: Category) : HomeUiAction
    /** Retour du Niveau 2 vers la liste des catégories. */
    data object BackToCategoriesList : HomeUiAction
    data class SelectChannel(val channel: Channel) : HomeUiAction
    data class SelectMatch(val match: MatchItem) : HomeUiAction
    data object DismissInstallDialog : HomeUiAction
    data object InstallStv : HomeUiAction
    data object Retry : HomeUiAction
    data object ToggleTheme : HomeUiAction
    /** null = afficher toutes les catégories. */
    data class SelectCategory(val categoryName: String?) : HomeUiAction
    data class UpdateSearchQuery(val query: String) : HomeUiAction
    data object ClearFilters : HomeUiAction
}

