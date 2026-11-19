package com.soufcom.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/** Réponse complète du catalogue distant. */
data class ChannelsCatalogResponseDto(
    @SerializedName("categories") val categories: List<CatalogCategoryDto>? = null,
    @SerializedName("channels") val channels: List<ChannelDto>? = null
)
