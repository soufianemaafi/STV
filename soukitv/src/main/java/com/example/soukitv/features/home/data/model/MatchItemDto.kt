package com.example.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/** Source de diffusion alternative d'un match, telle que renvoyée par le JSON distant. */
data class StreamSourceDto(
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("url") val url: String? = null
)

/** Match du jour tel que renvoyé par le JSON distant. */
data class MatchItemDto(
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
    @SerializedName("sources") val sources: List<StreamSourceDto>? = null
)

