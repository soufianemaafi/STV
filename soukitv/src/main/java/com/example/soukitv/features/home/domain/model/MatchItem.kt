package com.example.soukitv.features.home.domain.model

import com.google.gson.annotations.SerializedName

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
    @SerializedName("streamUrl") val streamUrl: String? = null
)

/** Convertit un match en [Channel] lisible par [com.example.soukitv.core.player.StvPlayerLauncher]. */
fun MatchItem.toChannel(): Channel = Channel(
    id = id.orEmpty(),
    name = "${teamA.orEmpty()} vs ${teamB.orEmpty()}",
    logoUrl = teamALogo.orEmpty(),
    streamUrl = streamUrl.orEmpty(),
    category = tournament.orEmpty()
)
