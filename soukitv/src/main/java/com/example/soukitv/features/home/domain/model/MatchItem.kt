package com.example.soukitv.features.home.domain.model


/** Source de diffusion alternative (qualité / serveur) d'un match. */
data class StreamSource(
    val quality: String,
    val url: String
)

data class MatchItem(
    val id: String? = null,
    val teamA: String? = null,
    val teamALogo: String? = null,
    val teamB: String? = null,
    val teamBLogo: String? = null,
    val time: String? = null,
    val tournament: String? = null,
    val channelName: String? = null,
    val commentator: String? = null,
    val streamUrl: String? = null,
    val sources: List<StreamSource>? = null
)

/** Convertit un match en [Channel] lisible par [com.example.soukitv.core.player.StvPlayerLauncher]. */
fun MatchItem.toChannel(): Channel = Channel(
    id = id.orEmpty(),
    name = "${teamA.orEmpty()} vs ${teamB.orEmpty()}",
    logoUrl = teamALogo.orEmpty(),
    streamUrl = streamUrl.orEmpty(),
    category = tournament.orEmpty()
)
/** Durée supposée d'un match, en minutes. */
private const val MATCH_DURATION_MINUTES = 120
/** Vrai si l'heure "HH:mm" du match est comprise dans la fenêtre [début, début + 2h[. */
fun MatchItem.isLiveAt(minutesOfDay: Int): Boolean {
    val parts = time?.split(":") ?: return false
    val start = (parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return false) * 60 +
        (parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return false)
    return minutesOfDay in start until start + MATCH_DURATION_MINUTES
}
