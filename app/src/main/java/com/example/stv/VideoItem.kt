package com.example.stv

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class VideoItem(
    val id: String = UUID.randomUUID().toString(),  // ✅ ID unique pour chaque vidéo
    val title: String,
    val url: String
)

