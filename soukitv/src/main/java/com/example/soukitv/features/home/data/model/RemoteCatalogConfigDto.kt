package com.example.soukitv.features.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * Configuration globale distante du catalogue.
 *
 * `minStvVersionCode` reste optionnel côté JSON et retombe sur 2L par défaut.
 */
data class RemoteCatalogConfigDto(
    @SerializedName("minStvVersionCode")
    val minStvVersionCode: Long = DEFAULT_MIN_STV_VERSION_CODE
) {
    companion object {
        const val DEFAULT_MIN_STV_VERSION_CODE = 2L
    }
}
