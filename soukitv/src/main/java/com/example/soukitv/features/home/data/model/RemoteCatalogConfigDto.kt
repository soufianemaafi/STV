package com.example.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * Configuration globale distante du catalogue.
 *
 * Les champs restent optionnels côté JSON pour permettre un fonctionnement dégradé hors ligne.
 */
data class RemoteCatalogConfigDto(
    @SerializedName("minStvVersionCode")
    val minStvVersionCode: Long = DEFAULT_MIN_STV_VERSION_CODE,
    @SerializedName("stvAuthSecret")
    val stvAuthSecret: String? = null
) {
    companion object {
        const val DEFAULT_MIN_STV_VERSION_CODE = 2L
    }
}
