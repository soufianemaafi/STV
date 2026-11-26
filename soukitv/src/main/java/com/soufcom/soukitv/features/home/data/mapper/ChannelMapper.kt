package com.soufcom.soukitv.features.home.data.mapper

import com.soufcom.soukitv.features.home.data.model.ChannelDto
import com.soufcom.soukitv.features.home.data.model.ChannelsCatalogResponseDto
import com.soufcom.soukitv.features.home.domain.model.Category
import com.soufcom.soukitv.features.home.domain.model.Channel

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
    val safeId = id?.trim().orEmpty()
    val safeName = name?.trim().orEmpty()
    val safeStreamUrl = streamUrl.toSafeHttpUrlOrNull()
    if (safeId.isEmpty() || safeName.isEmpty() || safeStreamUrl == null) return null

    val safeCategories = (categories.orEmpty() + category.orEmpty())
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .ifEmpty { listOf(DEFAULT_CATEGORY_NAME) }
    val primaryCategory = safeCategories.first()

    return Channel(
        id = safeId,
        name = safeName,
        logoUrl = logoUrl.toSafeHttpUrlOrNull() ?: "",
        streamUrl = safeStreamUrl,
        category = primaryCategory,
        country = country?.trim()?.takeIf { it.isNotEmpty() },
        categoryImageUrl = imageUrl.toSafeHttpUrlOrNull(),
        categories = safeCategories,
        primaryCategory = primaryCategory
    )
}

/** Convertit une liste de DTO en ignorant les entrées invalides. */
fun List<ChannelDto>.toDomain(): List<Channel> = mapNotNull { it.toDomain() }

/** Convertit la réponse hiérarchique en catégories dans l'ordre déclaré. */
fun ChannelsCatalogResponseDto.toCategories(): List<Category> {
    val validChannels = channels.orEmpty().toDomain()
        .groupBy { it.id }
        .values
        .map { sameId ->
            val first = sameId.first()
            first.copy(categories = sameId.flatMap { it.categories }.distinct())
        }
    val declaredNames = categories.orEmpty()
        .mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()
    val channelNames = validChannels.flatMap { it.categories }.distinct()
    val orderedNames = (declaredNames + channelNames).distinct()
    val metadataByName = categories.orEmpty().associateBy { it.name?.trim() }

    return orderedNames.map { name ->
        Category(
            name = name,
            channels = validChannels.filter { name in it.categories },
            imageUrl = metadataByName[name]?.imageUrl.toSafeHttpUrlOrNull()
                ?: validChannels.filter { name in it.categories }
                .firstOrNull { !it.categoryImageUrl.isNullOrBlank() }
                ?.categoryImageUrl
        )
    }
}

/** Regroupe une liste legacy de chaînes par catégorie, dans l'ordre d'apparition. */
fun List<Channel>.toCategories(): List<Category> =
    flatMap { channel ->
        channel.categories.ifEmpty { listOf(channel.primaryCategory) }.map { it to channel }
    }
        .groupBy({ it.first }, { it.second })
        .map { (name, channels) ->
            Category(
                name = name,
                channels = channels.distinctBy { it.id },
                imageUrl = channels.firstOrNull { !it.categoryImageUrl.isNullOrBlank() }?.categoryImageUrl
            )
        }

