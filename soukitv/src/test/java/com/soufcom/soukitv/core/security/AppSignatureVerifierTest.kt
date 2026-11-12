package com.soufcom.soukitv.core.security

import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSignatureVerifierTest {
    @Test
    fun isDigestEqual_acceptsTheExpectedSha256Digest() {
        val signatureBytes = "official-signing-certificate".toByteArray()
        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(signatureBytes)

        assertTrue(AppSignatureVerifier.isDigestEqual(signatureBytes, expectedDigest))
    }

    @Test
    fun isDigestEqual_rejectsADifferentCertificate() {
        val expectedDigest = MessageDigest.getInstance("SHA-256")
            .digest("official-signing-certificate".toByteArray())

        assertFalse(
            AppSignatureVerifier.isDigestEqual(
                "repacked-signing-certificate".toByteArray(),
                expectedDigest
            )
        )
    }
}

