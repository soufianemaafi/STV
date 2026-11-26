package com.soufcom.soukitv.features.home.data.mapper

import java.net.URI

/** Retourne l'URL trimmée si c'est une URI absolue HTTP(S) avec hôte ; sinon `null`. */
internal fun String?.toSafeHttpUrlOrNull(): String? {
    val candidate = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        URI(candidate).takeIf { uri ->
            (uri.scheme.equals("http", ignoreCase = true) ||
                uri.scheme.equals("https", ignoreCase = true)) &&
                !uri.host.isNullOrBlank()
        }?.toString()
    }.getOrNull()
}
