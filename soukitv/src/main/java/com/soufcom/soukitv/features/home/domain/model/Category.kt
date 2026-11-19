package com.soufcom.soukitv.features.home.domain.model

data class Category(
    val name: String,
    val channels: List<Channel>,
    val imageUrl: String? = null
) {
    val parent: String?
        get() = name.substringBefore('/', missingDelimiterValue = "").takeIf { '/' in name }

    val displayName: String
        get() = name.substringAfterLast('/')

    val channelCount: Int
        get() = channels.distinctBy { it.id }.size
}


