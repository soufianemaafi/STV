package com.example.soukitv.features.home.presentation

import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem

/** Onglets de la Bottom Navigation Bar. */
enum class NavigationTab { MATCHES, CHANNELS }

sealed interface HomeUiAction {
    data class SwitchTab(val tab: NavigationTab) : HomeUiAction
    /** Niveau 2 : ouvre la navigation d'une catégorie. */
    data class SelectCategoryDetail(val category: Category) : HomeUiAction
    /** Niveau 3 : ouvre la grille des chaînes d'un pays. */
    data class SelectCountry(val country: String) : HomeUiAction
    /** Retourne au niveau hiérarchique précédent. */
    data object NavigateBack : HomeUiAction
    data class SelectChannel(val channel: Channel) : HomeUiAction
    data class SelectMatch(val match: MatchItem) : HomeUiAction
    data object DismissInstallDialog : HomeUiAction
    data object InstallStv : HomeUiAction
    data object Retry : HomeUiAction
    data object ToggleTheme : HomeUiAction
}

