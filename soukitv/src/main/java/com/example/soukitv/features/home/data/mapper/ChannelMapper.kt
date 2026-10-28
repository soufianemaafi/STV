package com.example.soukitv.features.home.data.mapper

import com.example.soukitv.features.home.data.model.ChannelDto
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel

/** Catégorie par défaut quand le JSON distant n'en précise pas. */
const val DEFAULT_CATEGORY_NAME = "Autres"

/**
 * Convertit un [ChannelDto] en [Channel] du Domain.
 *
 * Retourne `null` si les champs indispensables à la lecture (nom, URL du flux) sont absents
 * ou vides : une entrée inutilisable est ignorée plutôt que d'afficher une carte cassée.
 * L'identifiant retombe sur l'URL du flux si le JSON n'en fournit pas.
 */
fun ChannelDto.toDomain(): Channel? {
    val safeName = name?.trim().orEmpty()
    val safeStreamUrl = streamUrl?.trim().orEmpty()
    if (safeName.isEmpty() || safeStreamUrl.isEmpty()) return null

    return Channel(
        id = id?.trim().takeUnless { it.isNullOrEmpty() } ?: safeStreamUrl,
        name = safeName,
        logoUrl = logoUrl?.trim().orEmpty(),
        streamUrl = safeStreamUrl,
        category = category?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_CATEGORY_NAME,
        country = country?.trim()?.takeIf { it.isNotEmpty() }
    )
}

/** Convertit une liste de DTO en ignorant les entrées invalides. */
fun List<ChannelDto>.toDomain(): List<Channel> = mapNotNull { it.toDomain() }

/** Regroupe des chaînes du Domain par catégorie (ordre d'apparition conservé). */
fun List<Channel>.toCategories(): List<Category> =
    groupBy { it.category }.map { (name, channels) -> Category(name, channels) }

