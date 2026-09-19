package com.example.stv.features.player.presentation

import com.example.stv.core.domain.model.VideoTrackInfo

/**
 * Actions MVI envoyées par la View (`PlayerActivity` / `VideoPlayer` composable)
 * vers le `PlayerViewModel`.
 *
 * L'Activity/UI ne fait AUCUNE logique métier ni AUCUN contrôle direct du player :
 * tout passe par `onAction(...)`. Le `Player` n'est exposé en lecture (attachement
 * de vue) que via `PlayerViewModel.player`.
 */
sealed class PlayerUiAction {

    /**
     * Une URL vidéo valide (déjà vérifiée par `IntentSecurityManager`) doit être chargée.
     * Ne déclenche PAS directement la lecture : décide seulement si des ads doivent
     * être affichées avant (état `LoadingAds`) ou si on passe direct à `Ready`.
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

    /**
     * Construit le `MediaItem` et lance réellement la lecture (`setMediaItem` + `prepare()`).
     * Dispatché une fois l'état `Ready` atteint (ads terminées ou `skipAds=true`).
     */
    data class PreparePlayback(val videoUrl: String) : PlayerUiAction()

    /** Reprend la lecture (`player.play()`). */
    data object Play : PlayerUiAction()

    /** Met en pause (`player.pause()`). */
    data object Pause : PlayerUiAction()

    /** Bascule Play ⇆ Pause selon l'état courant du player. */
    data object TogglePlayPause : PlayerUiAction()

    /** Déplace la lecture à une position précise (ms), avec clamping [0, duration]. */
    data class SeekTo(val positionMs: Long) : PlayerUiAction()

    /** Avance de 10 secondes. */
    data object SeekForward : PlayerUiAction()

    /** Recule de 10 secondes. */
    data object SeekRewind : PlayerUiAction()

    /** Sélectionne une piste vidéo (qualité) ou repasse en mode Auto. */
    data class SelectTrack(val trackInfo: VideoTrackInfo) : PlayerUiAction()
}



