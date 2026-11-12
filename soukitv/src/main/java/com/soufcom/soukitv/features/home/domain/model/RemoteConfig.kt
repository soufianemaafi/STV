package com.soufcom.soukitv.features.home.domain.model

/** Configuration distante nécessaire au catalogue et au lancement IPC de STV Player. */
data class RemoteConfig(
    val minVersionCode: Long,
    val authSecret: String = ""
)
