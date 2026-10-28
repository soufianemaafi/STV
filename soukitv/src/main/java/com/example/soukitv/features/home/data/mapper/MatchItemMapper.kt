package com.example.soukitv.features.home.data.mapper

import com.example.soukitv.features.home.data.model.MatchItemDto
import com.example.soukitv.features.home.data.model.StreamSourceDto
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource

/** Convertit une source de diffusion ; `null` si son URL est absente ou vide. */
fun StreamSourceDto.toDomain(): StreamSource? {
    val safeUrl = url?.trim().orEmpty()
    if (safeUrl.isEmpty()) return null
    return StreamSource(quality = quality?.trim().orEmpty(), url = safeUrl)
}

/** Convertit un [MatchItemDto] en [MatchItem] du Domain (le Domain accepte les champs absents). */
fun MatchItemDto.toDomain(): MatchItem = MatchItem(
    id = id,
    teamA = teamA,
    teamALogo = teamALogo,
    teamB = teamB,
    teamBLogo = teamBLogo,
    time = time,
    tournament = tournament,
    channelName = channelName,
    commentator = commentator,
    streamUrl = streamUrl,
    sources = sources?.mapNotNull { it.toDomain() }
)

/** Convertit une liste de DTO de matchs. */
fun List<MatchItemDto>.toDomain(): List<MatchItem> = map { it.toDomain() }

