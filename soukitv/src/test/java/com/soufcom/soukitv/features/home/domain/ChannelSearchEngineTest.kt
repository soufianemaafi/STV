package com.soufcom.soukitv.features.home.domain

import com.soufcom.soukitv.features.home.domain.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelSearchEngineTest {

    private fun channel(
        id: String,
        name: String,
        categories: List<String> = listOf("Autres"),
        country: String? = null
    ) = Channel(
        id = id,
        name = name,
        logoUrl = "",
        streamUrl = "https://example.com/$id.m3u8",
        categories = categories,
        country = country
    )

    private fun search(channels: List<Channel>, query: String): List<String> =
        ChannelSearchEngine.search(ChannelSearchEngine.index(channels), query).map { it.id }

    private val bein1 = channel("bein1", "beIN 1", listOf("Autres"))
    private val beinSports1 = channel("beinSports1", "beIN Sports 1", listOf("Autres"))
    private val mbc1 = channel("mbc1", "MBC 1")
    private val mbc2 = channel("mbc2", "MBC 2")
    private val lbc = channel("lbc", "LBC")
    private val twoM = channel("2m", "2M", listOf("Arabe/Maroc"), "Maroc")
    private val arryadia = channel("arryadia", "Arryadia", listOf("Sport/Maroc"), "Maroc")
    private val egypt = channel("egypt", "Al Hayat", listOf("Arabe/\u00C9gypte"), "\u00C9gypte")
    private val cinema = channel("cinema", "Ciel TV", listOf("Cin\u00E9ma"))
    private val chaine = channel("chaine", "Cha\u00EEne Info")

    @Test
    fun normalize_stripsAccentsPunctuationAndCase() {
        assertEquals("egypte", ChannelSearchEngine.normalize("\u00C9GYPTE"))
        assertEquals("arabe maroc", ChannelSearchEngine.normalize("Arabe/Maroc"))
        assertEquals("canal 2 hd", ChannelSearchEngine.normalize("  Canal+  2---HD "))
        assertEquals("bein1", ChannelSearchEngine.compact("beIN 1"))
    }

    @Test
    fun caseAndMultipleSpaces_areIgnored() {
        val result = search(listOf(mbc1, beinSports1), "  BEIN   sports  ")

        assertEquals(listOf("beinSports1"), result)
    }

    @Test
    fun latinAccents_matchBothWays() {
        val all = listOf(egypt, cinema, chaine, mbc1)

        assertEquals(listOf("egypt"), search(all, "egypte"))
        assertEquals(listOf("egypt"), search(all, "\u00C9gypte"))
        assertEquals(listOf("cinema"), search(all, "cinema"))
        assertEquals(listOf("chaine"), search(all, "chaine"))
    }

    @Test
    fun compactForm_matchesNamesWithSpacesAndRanksShortestFirst() {
        val result = search(listOf(beinSports1, bein1, mbc1), "bein1")

        assertEquals(listOf("bein1", "beinSports1"), result)
    }

    @Test
    fun compactForm_doesNotConfuseDigits() {
        val all = listOf(mbc1, mbc2, channel("mbc12", "MBC 12"))

        assertEquals(listOf("mbc2"), search(all, "mbc2"))
        assertEquals(listOf("mbc1"), search(all, "mbc1"))
        assertTrue(search(all, "mbc3").isEmpty())
    }

    @Test
    fun countryAndCategory_areSearchable() {
        val all = listOf(twoM, arryadia, mbc1)

        assertEquals(setOf("2m", "arryadia"), search(all, "maroc").toSet())
        assertEquals(listOf("arryadia"), search(all, "sport maroc"))
        assertEquals(listOf("2m"), search(all, "maroc 2m"))
        assertEquals(listOf("2m"), search(all, "2m maroc"))
    }

    @Test
    fun parentCategorySegment_isIndexed() {
        assertEquals(listOf("2m"), search(listOf(twoM, mbc1), "arabe"))
    }

    @Test
    fun exactNameMatch_isRankedFirst() {
        val all = listOf(
            channel("monde", "2M Monde"),
            channel("hd", "Arabe 2M"),
            twoM
        )

        val result = search(all, "2m")

        assertEquals("2m", result.first())
        assertEquals(setOf("2m", "monde", "hd"), result.toSet())
    }

    @Test
    fun relevance_exactThenPrefixThenContainsThenCategory() {
        val all = listOf(
            channel("category", "Arryadia", listOf("Sports")),
            channel("contains", "Eurosport"),
            channel("prefix", "Sport News"),
            channel("exact", "Sport")
        )

        assertEquals(
            listOf("exact", "prefix", "contains", "category"),
            search(all, "sport")
        )
    }

    @Test
    fun equalScores_keepOriginalOrder() {
        val all = listOf(
            channel("b", "Sport B"),
            channel("a", "Sport A"),
            channel("c", "Sport C")
        )

        assertEquals(listOf("b", "a", "c"), search(all, "sport"))
    }

    @Test
    fun typoTolerance_findsMisspelledWords() {
        val all = listOf(egypt, beinSports1, mbc1)

        assertEquals(listOf("egypt"), search(all, "egypt"))
        assertEquals(listOf("egypt"), search(all, "egipt"))
        assertEquals(listOf("beinSports1"), search(all, "beinsprot"))
    }

    @Test
    fun typoTolerance_neverOutranksRealMatches() {
        val all = listOf(channel("typo", "Spirt"), channel("real", "Sport Plus"))

        assertEquals(listOf("real", "typo"), search(all, "sport"))
    }

    @Test
    fun typoTolerance_ignoresShortTokensAndDigits() {
        assertEquals(listOf("mbc1"), search(listOf(lbc, mbc1), "mbc"))
        assertFalse("lbc" in search(listOf(lbc, mbc1), "mbc"))
        assertTrue(search(listOf(mbc2), "mbc3").isEmpty())
    }

    @Test
    fun arabic_normalizesHamzaTaaMarbutaAndTashkeel() {
        val jazeera = channel("jazeera", "\u0627\u0644\u062C\u0632\u064A\u0631\u0629")
        val akhbar = channel("akhbar", "\u0623\u062E\u0628\u0627\u0631")
        val all = listOf(jazeera, akhbar, mbc1)

        assertEquals(listOf("jazeera"), search(all, "\u0627\u0644\u062C\u0632\u064A\u0631\u0629"))
        assertEquals(listOf("jazeera"), search(all, "\u0627\u0644\u062C\u0632\u064A\u0631\u0647"))
        assertEquals(listOf("jazeera"), search(all, "\u0627\u0644\u062C\u0650\u0632\u064E\u064A\u0631\u0629"))
        assertEquals(listOf("akhbar"), search(all, "\u0627\u062E\u0628\u0627\u0631"))
        assertEquals(listOf("akhbar"), search(all, "\u0623\u062E\u0628\u0627\u0631"))
        assertEquals("\u0627", ChannelSearchEngine.normalize("\u0623"))
    }

    @Test
    fun blankQuery_returnsAllChannelsUnchanged() {
        assertEquals(listOf("mbc1", "lbc"), search(listOf(mbc1, lbc), "  "))
    }
}
