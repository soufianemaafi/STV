package com.stv.videoplayer.core.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class VideoItem(
    val id: String = UUID.randomUUID().toString(),  // âœ… ID unique pour chaque vidÃ©o
    val title: String,
    val url: String
)


