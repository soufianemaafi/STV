package com.example.soukitv.features.home.domain.model

import com.google.gson.annotations.SerializedName

data class MatchItem(
    @SerializedName("id") val id: String,
    @SerializedName("teamA") val teamA: String,
    @SerializedName("teamALogo") val teamALogo: String,
    @SerializedName("teamB") val teamB: String,
    @SerializedName("teamBLogo") val teamBLogo: String,
    @SerializedName("time") val time: String,
    @SerializedName("tournament") val tournament: String,
    @SerializedName("channelName") val channelName: String,
    @SerializedName("streamUrl") val streamUrl: String
)

/** Convertit un match en [Channel] lisible par [com.example.soukitv.core.player.StvPlayerLauncher]. */
fun MatchItem.toChannel(): Channel = Channel(
    id = id,
    name = "$teamA vs $teamB",
    logoUrl = teamALogo,
    streamUrl = streamUrl,
    category = tournament
)
