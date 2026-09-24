package com.stv.videoplayer.core.security

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour IntentSecurityManager.
 * VÃ©rifie la protection contre l'Intent Hijacking / Intent Redirection :
 * - Rejet strict de tout schÃ©ma diffÃ©rent de http/https (file://, content://, javascript:, ...).
 * - Acceptation des URLs https:// avec extensions vidÃ©o reconnues (.m3u8, .mp4, .mpd).
 * - Robustesse null-safe : aucun crash mÃªme avec des Intents/extras malformÃ©s.
 */
class IntentSecurityManagerTest {

    private lateinit var manager: IntentSecurityManager

    @Before
    fun setup() {
        manager = IntentSecurityManager()
    }

    // ==================== validateVideoUrl â€” Rejets (Intent Redirection) ====================

    @Test
    fun `URL file est rejetee`() {
        val result = manager.validateVideoUrl("file:///sdcard/secret.mp4")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL content est rejetee`() {
        val result = manager.validateVideoUrl("content://com.android.providers.media/video/1")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL javascript est rejetee`() {
        val result = manager.validateVideoUrl("javascript:alert(1)")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL intent scheme est rejetee`() {
        val result = manager.validateVideoUrl("intent://evil.app/#Intent;scheme=http;end")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL ftp est rejetee`() {
        val result = manager.validateVideoUrl("ftp://example.com/video.mp4")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL null est rejetee`() {
        val result = manager.validateVideoUrl(null)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL vide est rejetee`() {
        val result = manager.validateVideoUrl("   ")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL sans hote est rejetee`() {
        val result = manager.validateVideoUrl("https://")
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `URL trop longue est rejetee`() {
        val longUrl = "https://example.com/" + "a".repeat(2100)
        val result = manager.validateVideoUrl(longUrl)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    // ==================== validateVideoUrl â€” Acceptations ====================

    @Test
    fun `URL https avec extension m3u8 est acceptee`() {
        val result = manager.validateVideoUrl("https://example.com/stream.m3u8")
        assertTrue(result is IntentValidationResult.Valid)
        assertEquals("https://example.com/stream.m3u8", (result as IntentValidationResult.Valid).videoUrl)
    }

    @Test
    fun `URL https avec extension mp4 est acceptee`() {
        val result = manager.validateVideoUrl("https://example.com/video.mp4")
        assertTrue(result is IntentValidationResult.Valid)
    }

    @Test
    fun `URL https avec extension mpd est acceptee`() {
        val result = manager.validateVideoUrl("https://example.com/manifest.mpd")
        assertTrue(result is IntentValidationResult.Valid)
    }

    @Test
    fun `URL http simple est acceptee`() {
        val result = manager.validateVideoUrl("http://example.com/live")
        assertTrue(result is IntentValidationResult.Valid)
    }

    @Test
    fun `URL avec espaces autour est nettoyee et acceptee`() {
        val result = manager.validateVideoUrl("  https://example.com/video.mp4  ")
        assertTrue(result is IntentValidationResult.Valid)
    }

    // ==================== hasVideoExtension ====================

    @Test
    fun `hasVideoExtension detecte m3u8`() {
        assertTrue(manager.hasVideoExtension("https://example.com/stream.m3u8"))
    }

    @Test
    fun `hasVideoExtension detecte mp4 avec query params`() {
        assertTrue(manager.hasVideoExtension("https://example.com/video.mp4?token=abc"))
    }

    @Test
    fun `hasVideoExtension retourne false pour extension inconnue`() {
        assertFalse(manager.hasVideoExtension("https://example.com/page.html"))
    }

    @Test
    fun `hasVideoExtension retourne false pour null`() {
        assertFalse(manager.hasVideoExtension(null))
    }

    // ==================== validateIncomingIntent â€” robustesse Intent Hijacking ====================

    @Test
    fun `validateIncomingIntent avec intent null ne crash pas et rejette`() {
        val result = manager.validateIncomingIntent(null)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `validateIncomingIntent avec extra VIDEO_URL http valide est accepte`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns "https://example.com/stream.m3u8"

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Valid)
    }

    @Test
    fun `validateIncomingIntent avec extra VIDEO_URL malveillant est rejete`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns "file:///sdcard/secret.mp4"

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `validateIncomingIntent sans extra et sans data est rejete sans crash`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns null

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `validateIncomingIntent avec deep link stv play valide est accepte`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "stv"
        every { uri.host } returns "play"
        every { uri.getQueryParameter("url") } returns "https://stream.example.com/live.m3u8"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns uri

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Valid)
    }

    @Test
    fun `validateIncomingIntent avec deep link stv redirigeant vers file est rejete`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "stv"
        every { uri.host } returns "play"
        every { uri.getQueryParameter("url") } returns "file:///sdcard/secret.mp4"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns uri

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `validateIncomingIntent avec data URI content est rejete`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "content"
        every { uri.host } returns "com.android.providers.media"
        every { uri.toString() } returns "content://com.android.providers.media/video/1"

        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } returns null
        every { intent.data } returns uri

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Invalid)
    }

    @Test
    fun `validateIncomingIntent avec extension leve une exception est gere sans crash`() {
        val intent = mockk<Intent>()
        every { intent.getStringExtra("VIDEO_URL") } throws RuntimeException("Extras corrompus")
        every { intent.data } returns null

        val result = manager.validateIncomingIntent(intent)
        assertTrue(result is IntentValidationResult.Invalid)
    }
}


