package com.soufcom.soukitv.features.home.domain.model


/** Source de diffusion alternative (qualité / serveur) d'un match. */
data class StreamSource(
    val quality: String,
    val url: String
)

data class MatchItem(
    val id: String? = null,
    val teamA: String? = null,
    val teamALogo: String? = null,
    val teamB: String? = null,
    val teamBLogo: String? = null,
    val time: String? = null,
    val tournament: String? = null,
    val channelName: String? = null,
    val commentator: String? = null,
    val streamUrl: String? = null,
    val sources: List<StreamSource>? = null
)

/** Convertit un match en [Channel] lisible par [com.soufcom.soukitv.core.player.StvPlayerLauncher]. */
fun MatchItem.toChannel(): Channel = Channel(
    id = id.orEmpty(),
    name = "${teamA.orEmpty()} vs ${teamB.orEmpty()}",
    logoUrl = teamALogo.orEmpty(),
    streamUrl = streamUrl.orEmpty(),
    category = tournament.orEmpty()
)

/** Durée supposée d'un match, en minutes. */
private const val MATCH_DURATION_MINUTES = 120
private const val MINUTES_PER_DAY = 1440

/** Minutes depuis minuit pour une heure stricte "H:mm" / "HH:mm" ; null si absente ou invalide. */
fun MatchItem.parseStartMinutes(): Int? {
    val parts = time?.trim()?.split(":") ?: return null
    if (parts.size != 2) return null
    val (hourText, minuteText) = parts
    if (hourText.length !in 1..2 || minuteText.length != 2) return null
    if (!hourText.all(Char::isAsciiDigit) || !minuteText.all(Char::isAsciiDigit)) return null
    val hour = hourText.toInt()
    val minute = minuteText.toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

/** Vrai si l'instant est dans la fenêtre [début, début + 2h[, passage de minuit compris. */
fun MatchItem.isLiveAt(currentMinutes: Int): Boolean {
    if (currentMinutes !in 0 until MINUTES_PER_DAY) return false
    val startMinutes = parseStartMinutes() ?: return false
    val diff = (currentMinutes - startMinutes + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return diff in 0 until MATCH_DURATION_MINUTES
}
