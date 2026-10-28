package com.example.soukitv.features.home.domain.model

/** Chaîne TV : modèle métier pur (aucune dépendance Android / sérialisation). */
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val category: String,
    val country: String? = null
)


