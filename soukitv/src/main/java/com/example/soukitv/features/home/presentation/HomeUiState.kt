package com.example.soukitv.features.home.presentation
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val currentTab: NavigationTab = NavigationTab.MATCHES,
        val matches: List<MatchItem> = emptyList(),
        val categories: List<Category> = emptyList(),
        val activeCategoryDetail: Category? = null,
        val activeCountryDetail: String? = null,
        val isDarkTheme: Boolean = true,
        val searchQuery: String = "",
        /** Filtre actif (tournoi ou catégorie) ; null = "Tous". */
        val selectedFilter: String? = null,
        val filterOptions: List<String> = emptyList(),
        val visibleMatches: List<MatchItem> = emptyList(),
        /** Chaînes filtrées (favoris en tête) utilisées en mode liste plate. */
        val visibleChannels: List<Channel> = emptyList(),
        /** true si recherche/filtre actif : grille plate au lieu de la navigation hiérarchique. */
        val isFlatChannelMode: Boolean = false,
        val favorites: Set<String> = emptySet(),
        val liveMatchIds: Set<String> = emptySet()
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
