package com.soufcom.soukitv.core.security

object TestSecurityVault : SecurityVault {
    override fun nativeVerifyIntegrity(): Boolean = true

    override fun remoteUrl(key: Int): String = "https://test.invalid/"

    override fun trustedHosts(): Set<String> =
        setOf("196.200.156.190", "196.200.156.184", "196.200.156.186")

    override fun playerDigests(): List<ByteArray> = emptyList()
}
