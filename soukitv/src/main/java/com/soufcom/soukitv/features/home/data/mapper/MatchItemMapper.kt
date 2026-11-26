package com.soufcom.soukitv.features.home.data.mapper

import com.soufcom.soukitv.features.home.data.model.MatchItemDto
import com.soufcom.soukitv.features.home.data.model.StreamSourceDto
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.StreamSource

/** Convertit une source de diffusion ; `null` si son URL n'est pas une URL HTTP(S) valide. */
fun StreamSourceDto.toDomain(): StreamSource? {
    val safeUrl = url.toSafeHttpUrlOrNull() ?: return null
    return StreamSource(quality = quality?.trim().orEmpty(), url = safeUrl)
}

/** Convertit un [MatchItemDto] en [MatchItem] ; les URLs invalides deviennent `null`. */
fun MatchItemDto.toDomain(): MatchItem = MatchItem(
    id = id,
    teamA = teamA,
    teamALogo = teamALogo.toSafeHttpUrlOrNull(),
    teamB = teamB,
    teamBLogo = teamBLogo.toSafeHttpUrlOrNull(),
    time = time,
    tournament = tournament,
    channelName = channelName,
    commentator = commentator,
    streamUrl = streamUrl.toSafeHttpUrlOrNull(),
    sources = sources?.mapNotNull { it.toDomain() }
)

/** Convertit une liste de DTO ; garde la première occurrence des `id` non vides en doublon. */
fun List<MatchItemDto>.toDomain(): List<MatchItem> {
    val seenIds = HashSet<String>()
    return map { it.toDomain() }.filter { match ->
        val matchId = match.id?.trim().orEmpty()
        matchId.isEmpty() || seenIds.add(matchId)
    }
}

