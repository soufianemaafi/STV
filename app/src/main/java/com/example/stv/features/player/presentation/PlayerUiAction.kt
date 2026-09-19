package com.example.stv.features.player.presentation

/**
 * Actions MVI envoyées par la View (`PlayerActivity`) vers le `PlayerViewModel`.
 *
 * L'Activity ne fait AUCUNE logique métier : elle délègue la validation de
 * l'Intent entrant à `IntentSecurityManager`, puis transforme le résultat en
 * une de ces actions avant de l'envoyer au ViewModel via `onAction(...)`.
 */
sealed class PlayerUiAction {

    /**
     * Une URL vidéo valide (déjà vérifiée par `IntentSecurityManager`) doit être chargée.
     * @param videoUrl URL http/https validée.
     * @param skipAds Si true, saute l'affichage de la publicité.
     */
    data class LoadVideo(val videoUrl: String, val skipAds: Boolean) : PlayerUiAction()

    /**
     * L'Intent entrant a été jugé invalide ou potentiellement malveillant
     * (schéma interdit, URL malformée, extras corrompus, etc.).
     * @param userMessage Message générique et sûr à afficher à l'utilisateur
     * (ne JAMAIS refléter le contenu brut de l'intent dans l'UI).
     */
    data class RejectInvalidIntent(val userMessage: String) : PlayerUiAction()
}

