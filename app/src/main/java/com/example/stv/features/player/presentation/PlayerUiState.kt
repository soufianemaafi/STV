package com.example.stv.features.player.presentation

/**
 * State machine pour l'écran du player.
 *
 * Flux : LoadingAds → ShowingAd → Ready
 *        LoadingAds → AdBlockerBlocked / NeedRetry (pub échoue, bouton Réessayer)
 *        LoadingAds → NetworkError (pas de réseau, bouton Réessayer)
 *        Error (URL invalide)
 */
sealed class PlayerUiState {
    data class LoadingAds(val videoUrl: String, val forceAdCheck: Boolean = false) : PlayerUiState()
    data class ShowingAd(val videoUrl: String) : PlayerUiState()
    data class Ready(val videoUrl: String) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()

    /** Blocage suspecté côté pub / AdBlocker — conserve l'UI existante avec Réessayer. */
    data class AdBlockerBlocked(val videoUrl: String) : PlayerUiState()

    /** Pub échouée après 3 tentatives auto — bouton Réessayer affiché */
    data class NeedRetry(val videoUrl: String) : PlayerUiState()

    /** Pas de réseau — bouton Réessayer affiché */
    data class NetworkError(val videoUrl: String) : PlayerUiState()
}

