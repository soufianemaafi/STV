package com.stv.videoplayer.player

import android.content.Intent
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM purs de [HeaderExtractor] : le coeur [HeaderExtractor.parse] est testé directement,
 * les chemins `Bundle` / `Intent` via des mocks (aucun Robolectric requis).
 */
@Suppress("DEPRECATION")
class HeaderExtractorTest {

    // ==================== Formats d'entrée ====================

    @Test
    fun `une Map est assainie et les noms connus sont canonicalises`() {
        val result = HeaderExtractor.parse(
            mapOf("referer" to "https://partner.example.com", "USER-AGENT" to "CustomAgent/1.0", "X-Token" to "abc")
        )

        assertEquals(
            mapOf(
                "Referer" to "https://partner.example.com",
                "User-Agent" to "CustomAgent/1.0",
                "X-Token" to "abc"
            ),
            result
        )
    }

    @Test
    fun `un tableau accepte les entrees Nom colon valeur et Nom egal valeur`() {
        val result = HeaderExtractor.parse(arrayOf("X-One: 1", "X-Two=2"))

        assertEquals(mapOf("X-One" to "1", "X-Two" to "2"), result)
    }

    @Test
    fun `un Iterable est supporte`() {
        val result = HeaderExtractor.parse(listOf("X-One: 1", "X-Two: 2"))

        assertEquals(mapOf("X-One" to "1", "X-Two" to "2"), result)
    }

    @Test
    fun `une String multi-lignes donne une entree par ligne`() {
        val result = HeaderExtractor.parse("X-One: 1\r\nX-Two: 2\nX-Three=3")

        assertEquals(mapOf("X-One" to "1", "X-Two" to "2", "X-Three" to "3"), result)
    }

    @Test
    fun `le premier separateur est utilise pour preserver les valeurs contenant egal ou deux-points`() {
        val result = HeaderExtractor.parse(
            arrayOf("Authorization: Basic abc==", "Referer=https://example.com/a?b=c")
        )

        assertEquals("Basic abc==", result["Authorization"])
        assertEquals("https://example.com/a?b=c", result["Referer"])
    }

    @Test
    fun `un Bundle imbrique est lu cle par cle`() {
        val nested = mockk<Bundle>()
        every { nested.keySet() } returns linkedSetOf("X-A", "X-B")
        every { nested.get("X-A") } returns "1"
        every { nested.get("X-B") } returns 2

        val result = HeaderExtractor.parse(nested)

        assertEquals(mapOf("X-A" to "1", "X-B" to "2"), result)
    }

    // ==================== Referer ====================

    @Test
    fun `les extras referer sont prioritaires sur un header Referer explicite`() {
        val result = HeaderExtractor.parse(mapOf("Referer" to "https://old.example.com"), "https://new.example.com")

        assertEquals(mapOf("Referer" to "https://new.example.com"), result)
    }

    @Test
    fun `les noms sont dedoublonnes sans tenir compte de la casse`() {
        val result = HeaderExtractor.parse(linkedMapOf("x-token" to "a", "X-TOKEN" to "b"))

        assertEquals(1, result.size)
        assertEquals("b", result.values.single())
    }

    // ==================== Securite ====================

    @Test
    fun `CR et LF dans une valeur sont neutralises contre l'injection de header`() {
        val result = HeaderExtractor.parse(mapOf("X-Test" to "ok\r\nInjected: evil"))

        val value = result.getValue("X-Test")
        assertTrue(!value.contains('\r') && !value.contains('\n'))
        assertEquals(1, result.size)
    }

    @Test
    fun `un nom de header invalide est rejete`() {
        val result = HeaderExtractor.parse(
            mapOf(
                "Bad Name" to "1",
                "Bad:Name" to "2",
                "X\r\nInjected" to "3",
                "" to "4",
                "Good-Name" to "5"
            )
        )

        assertEquals(mapOf("Good-Name" to "5"), result)
    }

    @Test
    fun `les caracteres de controle sont retires des valeurs`() {
        val result = HeaderExtractor.parse(mapOf("X-Test" to "\u0000ab\u0007c\u007F"))

        assertEquals(mapOf("X-Test" to "abc"), result)
    }

    @Test
    fun `les valeurs vides ou blanches sont ignorees`() {
        val result = HeaderExtractor.parse(mapOf("X-Empty" to "", "X-Blank" to "   ", "X-Ok" to "1"))

        assertEquals(mapOf("X-Ok" to "1"), result)
    }

    @Test
    fun `les entrees sans separateur ou sans valeur sont ignorees`() {
        val result = HeaderExtractor.parse(arrayOf("NoSeparator", ":NoName", "X-NoValue:", "X-Ok: 1"))

        assertEquals(mapOf("X-Ok" to "1"), result)
    }

    @Test
    fun `une valeur null ou d'un type non supporte donne une map vide`() {
        assertTrue(HeaderExtractor.parse(null).isEmpty())
        assertTrue(HeaderExtractor.parse(42).isEmpty())
        assertTrue(HeaderExtractor.parse(Any()).isEmpty())
    }

    // ==================== Bundle / Intent ====================

    @Test
    fun `un Bundle null donne une map vide`() {
        assertTrue(HeaderExtractor.extract(null as Bundle?).isEmpty())
    }

    @Test
    fun `extract lit les extras headers puis referer depuis un Bundle`() {
        val extras = mockk<Bundle>()
        every { extras.get("headers") } returns mapOf("X-Token" to "abc")
        every { extras.get("referer") } returns "https://r.example.com"
        every { extras.get("Referer") } returns null

        val result = HeaderExtractor.extract(extras)

        assertEquals(mapOf("X-Token" to "abc", "Referer" to "https://r.example.com"), result)
    }

    @Test
    fun `un extra corrompu ne fait jamais crasher l'extraction`() {
        val extras = mockk<Bundle>()
        every { extras.get(any()) } throws RuntimeException("BadParcelableException")

        assertTrue(HeaderExtractor.extract(extras).isEmpty())
    }

    @Test
    fun `des extras d'Intent illisibles donnent une map vide`() {
        val intent = mockk<Intent>()
        every { intent.extras } throws RuntimeException("corrupted extras")

        assertTrue(HeaderExtractor.extract(intent).isEmpty())
    }

    @Test
    fun `un Intent null donne une map vide`() {
        assertTrue(HeaderExtractor.extract(null as Intent?).isEmpty())
    }

    @Test
    fun `l'extraction est sans etat entre deux appels`() {
        val first = HeaderExtractor.parse(mapOf("X-Session" to "one"))
        val second = HeaderExtractor.parse(emptyMap<String, String>())

        assertEquals(mapOf("X-Session" to "one"), first)
        assertTrue(second.isEmpty())
    }
}

