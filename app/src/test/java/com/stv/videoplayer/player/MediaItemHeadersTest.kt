package com.stv.videoplayer.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM purs du transport des headers dans `MediaItem.RequestMetadata.extras`.
 * (`Bundle` est stubbé en JVM : le chemin de lecture est donc validé avec un `Bundle` mocké.)
 */
@Suppress("DEPRECATION")
class MediaItemHeadersTest {

    @Test
    fun `write sans header exploitable laisse le MediaItem sans extras`() {
        val item = MediaItemHeaders.write(MediaItem.Builder(), emptyMap()).build()

        assertNull(item.requestMetadata.extras)
    }

    @Test
    fun `write avec uniquement des headers invalides laisse le MediaItem sans extras`() {
        val item = MediaItemHeaders.write(MediaItem.Builder(), mapOf("Bad Name" to "1", "X-Blank" to " ")).build()

        assertNull(item.requestMetadata.extras)
    }

    @Test
    fun `write place les headers dans RequestMetadata extras`() {
        val item = MediaItemHeaders.write(
            MediaItem.Builder(),
            mapOf("Referer" to "https://partner.example.com")
        ).build()

        assertNotNull(item.requestMetadata.extras)
    }

    @Test
    fun `read d'un MediaItem sans metadata retourne une map vide`() {
        assertTrue(MediaItemHeaders.read(MediaItem.Builder().build()).isEmpty())
    }

    @Test
    fun `read relit et reassainit les headers portes par le MediaItem`() {
        val extras = mockk<Bundle>()
        every { extras.get("headers") } returns mapOf(
            "Referer" to "https://partner.example.com",
            "Bad Name" to "rejected",
            "X-Token" to "abc\r\nInjected: evil"
        )
        every { extras.get("referer") } returns null
        every { extras.get("Referer") } returns null
        val item = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(extras).build())
            .build()

        val headers = MediaItemHeaders.read(item)

        assertEquals("https://partner.example.com", headers["Referer"])
        assertTrue(!headers.containsKey("Bad Name"))
        assertTrue(headers.getValue("X-Token").none { it == '\r' || it == '\n' })
    }

    @Test
    fun `read est propre a chaque MediaItem sans etat partage`() {
        val withHeaders = mockk<Bundle>()
        every { withHeaders.get("headers") } returns mapOf("X-Session" to "one")
        every { withHeaders.get("referer") } returns null
        every { withHeaders.get("Referer") } returns null
        val itemA = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(withHeaders).build())
            .build()
        val itemB = MediaItem.Builder().build()

        assertEquals(mapOf("X-Session" to "one"), MediaItemHeaders.read(itemA))
        assertTrue(MediaItemHeaders.read(itemB).isEmpty())
        assertEquals(mapOf("X-Session" to "one"), MediaItemHeaders.read(itemA))
    }

    @Test
    fun `la factory par item chaine bien ses setters`() {
        val factory = HeaderAwareMediaSourceFactory()

        assertSame(
            factory,
            factory.setLoadErrorHandlingPolicy(mockk(relaxed = true))
        )
        assertSame(
            factory,
            factory.setDrmSessionManagerProvider(mockk(relaxed = true))
        )
    }
}

