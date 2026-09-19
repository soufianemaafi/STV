package com.example.stv

import com.example.stv.core.domain.model.VideoItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour VideoItem.
 * Vérifie la sérialisation/désérialisation JSON et l'ID unique.
 */
class VideoItemTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `chaque VideoItem a un ID unique`() {
        val item1 = VideoItem(title = "Test 1", url = "https://example.com/1.m3u8")
        val item2 = VideoItem(title = "Test 2", url = "https://example.com/2.m3u8")
        assertNotEquals(item1.id, item2.id)
    }

    @Test
    fun `VideoItem avec meme titre et URL ont des IDs differents`() {
        val item1 = VideoItem(title = "Same", url = "https://example.com/same.m3u8")
        val item2 = VideoItem(title = "Same", url = "https://example.com/same.m3u8")
        assertNotEquals(item1.id, item2.id)
    }

    @Test
    fun `serialization round-trip preserve les donnees`() {
        val original = VideoItem(title = "Test", url = "https://example.com/stream.m3u8")
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<VideoItem>(encoded)

        assertEquals(original.id, decoded.id)
        assertEquals(original.title, decoded.title)
        assertEquals(original.url, decoded.url)
    }

    @Test
    fun `serialization d'une liste fonctionne`() {
        val list = listOf(
            VideoItem(title = "Video 1", url = "https://example.com/1.m3u8"),
            VideoItem(title = "Video 2", url = "https://example.com/2.m3u8")
        )
        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<List<VideoItem>>(encoded)

        assertEquals(2, decoded.size)
        assertEquals("Video 1", decoded[0].title)
        assertEquals("Video 2", decoded[1].title)
    }

    @Test
    fun `deserialization avec cles inconnues ne crash pas`() {
        val jsonStr = """{"id":"abc","title":"Test","url":"https://example.com","unknownKey":"value"}"""
        val item = json.decodeFromString<VideoItem>(jsonStr)
        assertEquals("Test", item.title)
        assertEquals("https://example.com", item.url)
    }

    @Test
    fun `ID est inclus dans la serialization`() {
        val item = VideoItem(title = "Test", url = "https://example.com")
        val encoded = json.encodeToString(item)
        assertTrue(encoded.contains("\"id\""))
        assertTrue(encoded.contains(item.id))
    }
}

