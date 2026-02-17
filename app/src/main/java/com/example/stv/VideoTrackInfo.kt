package com.example.stv

data class VideoTrackInfo(
    val name: String,
    val group: androidx.media3.common.TrackGroup?,
    val trackIndex: Int?,
    val height: Int = -1,
    val bitrate: Int = -1
)

