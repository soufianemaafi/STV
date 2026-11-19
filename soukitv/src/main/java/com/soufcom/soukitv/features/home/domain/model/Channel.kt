package com.soufcom.soukitv.features.home.domain.model

/** Chaîne TV : modèle métier pur (aucune dépendance Android / sérialisation). */
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    /** Champ historique conservé pour les consommateurs existants. */
    val category: String = "Autres",
    val country: String? = null,
    val categoryImageUrl: String? = null,
    /** Tous les chemins de catégories associés à cette chaîne, sans doublons. */
    val categories: List<String> = listOf(category),
    /** Catégorie utilisée pour le libellé compact des tuiles et les fallbacks legacy. */
    val primaryCategory: String = categories.firstOrNull() ?: category
)


