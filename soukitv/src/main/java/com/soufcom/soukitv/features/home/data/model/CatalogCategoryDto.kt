package com.soufcom.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/** Métadonnées d'une catégorie déclarée dans channels.json. */
data class CatalogCategoryDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null
)
