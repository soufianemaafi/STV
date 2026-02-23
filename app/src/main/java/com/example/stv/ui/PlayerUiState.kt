package com.example.stv.ui

/**
 * State machine pour l'écran du player.
 * Permet de gérer les différentes phases : chargement des ads, fallback, lecture, erreur.
 */
sealed class PlayerUiState {
    /** Phase initiale : chargement de la publicité interstitielle. */
    data object LoadingAds : PlayerUiState()

    /** La pub n'a pas pu être chargée (erreur réseau, timeout) → afficher bannière. */
    data class Fallback(val adBlockDetected: Boolean = false) : PlayerUiState()

    /** Prêt à lancer la vidéo. */
    data class Ready(val videoUrl: String) : PlayerUiState()

    /** Erreur : URL manquante ou invalide. */
    data class Error(val message: String) : PlayerUiState()
}

