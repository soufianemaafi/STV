package com.example.soukitv.features.home.domain.model

import com.google.gson.annotations.SerializedName

data class Channel(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("logoUrl") val logoUrl: String,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("category") val category: String
)


