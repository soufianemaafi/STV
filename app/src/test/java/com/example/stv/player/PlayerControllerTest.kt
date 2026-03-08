package com.example.stv.player

import android.content.Context
import android.content.Intent
import com.example.stv.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour PlayerController.
 * Vérifie la validation d'URL et la résolution d'URL depuis les Intents.
 *
 * Note : android.util.Patterns n'est pas disponible dans les tests JVM purs.
 * Les tests vérifient les rejets (null, vide, mauvais schéma, trop long)
 * qui sont traités AVANT l'appel à Patterns.WEB_URL.
 */
class PlayerControllerTest {

    private lateinit var context: Context
    private lateinit var controller: PlayerController

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        every { context.getString(R.string.error_url_missing) } returns "URL missing"
        every { context.getString(R.string.error_url_invalid_scheme) } returns "Invalid scheme"
        every { context.getString(R.string.error_url_too_long) } returns "URL too long"
        every { context.getString(R.string.error_url_invalid_format) } returns "Invalid format"
        controller = PlayerController(context)
    }

    // ==================== validateStreamUrl — Rejets ====================

    @Test
    fun `URL null retourne erreur missing`() {
        val result = controller.validateStreamUrl(null)
        assertEquals("URL missing", result)
    }

    @Test
    fun `URL vide retourne erreur missing`() {
        val result = controller.validateStreamUrl("")
        assertEquals("URL missing", result)
    }

    @Test
    fun `URL avec espaces seulement retourne erreur missing`() {
        val result = controller.validateStreamUrl("   ")
        assertEquals("URL missing", result)
    }

    @Test
    fun `URL sans schema retourne erreur scheme`() {
        val result = controller.validateStreamUrl("example.com/stream.m3u8")
        assertEquals("Invalid scheme", result)
    }

    @Test
    fun `URL FTP retourne erreur scheme`() {
        val result = controller.validateStreamUrl("ftp://example.com/video.mp4")
        assertEquals("Invalid scheme", result)
    }

    @Test
    fun `URL RTSP retourne erreur scheme`() {
        val result = controller.validateStreamUrl("rtsp://example.com/live")
        assertEquals("Invalid scheme", result)
    }

    @Test
    fun `URL trop longue retourne erreur too long`() {
        val longUrl = "https://example.com/" + "a".repeat(2030)
        val result = controller.validateStreamUrl(longUrl)
        assertEquals("URL too long", result)
    }

    // Note : Les tests d'URL valides et le test Patterns.WEB_URL
    // nécessitent un émulateur Android (tests instrumentés)
    // car android.util.Patterns n'est pas disponible en JVM pure.

    // ==================== isValidStreamUrl ====================

    @Test
    fun `isValidStreamUrl retourne false pour URL null`() {
        assertFalse(controller.isValidStreamUrl(null))
    }

    @Test
    fun `isValidStreamUrl retourne false pour URL vide`() {
        assertFalse(controller.isValidStreamUrl(""))
    }

    @Test
    fun `isValidStreamUrl retourne false pour URL sans schema`() {
        assertFalse(controller.isValidStreamUrl("example.com/stream"))
    }

    // ==================== resolveVideoUrl ====================

    @Test
    fun `resolveVideoUrl avec extra VIDEO_URL retourne la valeur`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns "https://test.com/stream.m3u8"
        every { intent.data } returns null

        val result = controller.resolveVideoUrl(intent)
        assertEquals("https://test.com/stream.m3u8", result)
    }

    @Test
    fun `resolveVideoUrl sans extra et sans data retourne null`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns null

        val result = controller.resolveVideoUrl(intent)
        assertNull(result)
    }

    @Test
    fun `resolveVideoUrl avec intent null retourne null`() {
        val result = controller.resolveVideoUrl(null)
        assertNull(result)
    }

    @Test
    fun `resolveVideoUrl avec extra vide tombe sur data URI`() {
        val uri = mockk<android.net.Uri>()
        every { uri.scheme } returns "https"
        every { uri.host } returns "example.com"
        every { uri.toString() } returns "https://example.com/video.mp4"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns ""
        every { intent.data } returns uri

        val result = controller.resolveVideoUrl(intent)
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun `resolveVideoUrl avec deep link stv schema`() {
        val uri = mockk<android.net.Uri>()
        every { uri.scheme } returns "stv"
        every { uri.host } returns "play"
        every { uri.getQueryParameter("url") } returns "https://stream.example.com/live.m3u8"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns uri

        val result = controller.resolveVideoUrl(intent)
        assertEquals("https://stream.example.com/live.m3u8", result)
    }

    @Test
    fun `resolveVideoUrl prefere extra VIDEO_URL sur data URI`() {
        val uri = mockk<android.net.Uri>()
        every { uri.toString() } returns "https://data-uri.com/video.mp4"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns "https://extra-url.com/stream.m3u8"
        every { intent.data } returns uri

        val result = controller.resolveVideoUrl(intent)
        // L'extra a priorité sur data URI
        assertEquals("https://extra-url.com/stream.m3u8", result)
    }
}

