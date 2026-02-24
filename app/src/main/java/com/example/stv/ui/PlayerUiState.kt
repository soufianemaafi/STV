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

    /**
     * Pub interstitielle en cours d'affichage.
     * Le player NE DOIT PAS démarrer tant que la pub n'est pas fermée.
     * Optionnel : préchargement silencieux du flux (mute + pause).
     */
    data class ShowingAd(val videoUrl: String) : PlayerUiState()

    /** Prêt à lancer la vidéo (pub fermée ou skippée). */
    data class Ready(val videoUrl: String) : PlayerUiState()

    /** Erreur : URL manquante ou invalide. */
    data class Error(val message: String) : PlayerUiState()

    /**
     * Bloqué : Adblock détecté après plusieurs tentatives.
     * L'utilisateur NE PEUT PAS accéder au contenu.
     * Affiche un message et ferme l'app.
     */
    data object Blocked : PlayerUiState()
}

