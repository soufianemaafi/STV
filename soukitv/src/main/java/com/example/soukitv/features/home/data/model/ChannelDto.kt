package com.example.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * Chaîne telle que renvoyée par le catalogue JSON distant.
 *
 * Tous les champs sont nullables : Gson contourne la null-safety de Kotlin et un JSON
 * incomplet ne doit jamais faire crasher l'application. La validation se fait dans le mapper.
 */
data class ChannelDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("logoUrl") val logoUrl: String? = null,
    @SerializedName("streamUrl") val streamUrl: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("country") val country: String? = null
)

