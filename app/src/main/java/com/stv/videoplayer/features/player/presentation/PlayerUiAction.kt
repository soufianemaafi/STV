package com.stv.videoplayer.features.player.presentation

import com.stv.videoplayer.core.domain.model.StreamSource
import com.stv.videoplayer.core.domain.model.VideoTrackInfo

/**
 * Actions MVI envoyÃ©es par la View (`PlayerActivity` / `VideoPlayer` composable)
 * vers le `PlayerViewModel`.
 *
 * L'Activity/UI ne fait AUCUNE logique mÃ©tier ni AUCUN contrÃ´le direct du player :
 * tout passe par `onAction(...)`. Le `Player` n'est exposÃ© en lecture (attachement
 * de vue) que via `PlayerViewModel.player`.
 */
sealed class PlayerUiAction {

    /**
     * Une URL vidÃ©o valide (dÃ©jÃ  vÃ©rifiÃ©e par `IntentSecurityManager`) doit Ãªtre chargÃ©e.
     * Ne dÃ©clenche PAS directement la lecture : dÃ©cide seulement si des ads doivent
     * Ãªtre affichÃ©es avant (Ã©tat `LoadingAds`) ou si on passe direct Ã  `Ready`.
     * @param videoUrl URL http/https validÃ©e.
     * @param skipAds Si true, saute l'affichage de la publicitÃ©.
     * @param title Titre convivial du flux, si fourni par l'Intent.
     * @param headers Headers HTTP optionnels Ã  propager vers Media3.
     */
    data class LoadVideo(
        val videoUrl: String,
        val skipAds: Boolean = false,
        val title: String? = null,
        val headers: Map<String, String> = emptyMap(),
        val sources: List<StreamSource> = emptyList(),
        /** MIME type fourni par l'Intent (ex. HLS), optionnel. */
        val mimeType: String? = null
    ) : PlayerUiAction()

    /**
     * Bascule vers une autre source (serveur / qualité) d'un match en conservant
     * la position de lecture courante.
     */
    data class SwitchSource(val sourceUrl: String, val qualityIndex: Int) : PlayerUiAction()

    /** Relance proprement la vÃ©rification pub / AdBlocker via MVI. */
    data object RetryAdCheck : PlayerUiAction()

    /**
     * L'Intent entrant a Ã©tÃ© jugÃ© invalide ou potentiellement malveillant
     * (schÃ©ma interdit, URL malformÃ©e, extras corrompus, etc.).
     * @param userMessage Message gÃ©nÃ©rique et sÃ»r Ã  afficher Ã  l'utilisateur
     * (ne JAMAIS reflÃ©ter le contenu brut de l'intent dans l'UI).
     */
    data class RejectInvalidIntent(val userMessage: String) : PlayerUiAction()

    /**
     * Construit le `MediaItem` et lance rÃ©ellement la lecture (`setMediaItem` + `prepare()`).
     * DispatchÃ© une fois l'Ã©tat `Ready` atteint (ads terminÃ©es ou `skipAds=true`).
     */
    data class PreparePlayback(
        val videoUrl: String,
        /**
         * Headers liés à CETTE URL. `null` = utiliser ceux de la session courante, mais
         * uniquement si [videoUrl] en est bien l'URL (sinon l'action est périmée et ignorée).
         */
        val headers: Map<String, String>? = null
    ) : PlayerUiAction()

    /** L'écran n'est plus visible (`ON_STOP`) : pause, sauf en Picture-in-Picture. */
    data class Background(val isInPictureInPicture: Boolean) : PlayerUiAction()

    /** L'écran est de nouveau visible (`ON_START`) : reprend si la lecture était active. */
    data object Foreground : PlayerUiAction()

    /** Reprend la lecture (`player.play()`). */
    data object Play : PlayerUiAction()

    /** Met en pause (`player.pause()`). */
    data object Pause : PlayerUiAction()

    /** Bascule Play â‡† Pause selon l'Ã©tat courant du player. */
    data object TogglePlayPause : PlayerUiAction()

    /** DÃ©place la lecture Ã  une position prÃ©cise (ms), avec clamping [0, duration]. */
    data class SeekTo(val positionMs: Long) : PlayerUiAction()

    /** Avance de 10 secondes. */
    data object SeekForward : PlayerUiAction()

    /** Recule de 10 secondes. */
    data object SeekRewind : PlayerUiAction()

    /** SÃ©lectionne une piste vidÃ©o (qualitÃ©) ou repasse en mode Auto. */
    data class SelectTrack(val trackInfo: VideoTrackInfo) : PlayerUiAction()
}




