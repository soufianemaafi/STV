package com.example.soukitv.features.home.domain.model

import com.google.gson.annotations.SerializedName

/** Source de diffusion alternative (qualité / serveur) d'un match. */
data class StreamSource(
    @SerializedName("quality") val quality: String,
    @SerializedName("url") val url: String
)

data class MatchItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("teamA") val teamA: String? = null,
    @SerializedName("teamALogo") val teamALogo: String? = null,
    @SerializedName("teamB") val teamB: String? = null,
    @SerializedName("teamBLogo") val teamBLogo: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("tournament") val tournament: String? = null,
    @SerializedName("channelName") val channelName: String? = null,
    @SerializedName("commentator") val commentator: String? = null,
    @SerializedName("streamUrl") val streamUrl: String? = null,
    @SerializedName("sources") val sources: List<StreamSource>? = null
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
