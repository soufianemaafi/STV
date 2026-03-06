package com.example.stv

import java.util.UUID

data class VideoItem(
    val id: String = UUID.randomUUID().toString(),  // ✅ ID unique pour chaque vidéo
    val title: String,
    val url: String
)

