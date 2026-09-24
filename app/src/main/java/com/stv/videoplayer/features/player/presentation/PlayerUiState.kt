package com.stv.videoplayer.features.player.presentation

/**
 * State machine pour l'Ã©cran du player.
 *
 * Flux : LoadingAds â†’ ShowingAd â†’ Ready
 *        LoadingAds â†’ AdBlockerBlocked / NeedRetry (pub Ã©choue, bouton RÃ©essayer)
 *        LoadingAds â†’ NetworkError (pas de rÃ©seau, bouton RÃ©essayer)
 *        Error (URL invalide)
 */
sealed class PlayerUiState {
    data class LoadingAds(val videoUrl: String, val forceAdCheck: Boolean = false) : PlayerUiState()
    data class ShowingAd(val videoUrl: String) : PlayerUiState()
    data class Ready(val videoUrl: String) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()

    /** Blocage suspectÃ© cÃ´tÃ© pub / AdBlocker â€” conserve l'UI existante avec RÃ©essayer. */
    data class AdBlockerBlocked(val videoUrl: String) : PlayerUiState()

    /** Pub Ã©chouÃ©e aprÃ¨s 3 tentatives auto â€” bouton RÃ©essayer affichÃ© */
    data class NeedRetry(val videoUrl: String) : PlayerUiState()

    /** Pas de rÃ©seau â€” bouton RÃ©essayer affichÃ© */
    data class NetworkError(val videoUrl: String) : PlayerUiState()
}


