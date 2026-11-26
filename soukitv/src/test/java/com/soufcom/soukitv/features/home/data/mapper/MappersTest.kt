package com.soufcom.soukitv.features.home.data.mapper

import com.soufcom.soukitv.features.home.data.model.ChannelDto
import com.soufcom.soukitv.features.home.data.model.ChannelsCatalogResponseDto
import com.soufcom.soukitv.features.home.data.model.MatchItemDto
import com.soufcom.soukitv.features.home.data.model.StreamSourceDto
import com.soufcom.soukitv.features.home.domain.model.Category
import com.soufcom.soukitv.features.home.domain.model.Channel
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.StreamSource
import com.soufcom.soukitv.features.home.domain.model.isLiveAt
import com.soufcom.soukitv.features.home.domain.model.parseStartMinutes
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun channelDto_mapsAllFieldsToDomain() {
        val dto = ChannelDto("7", " France 24 ", "https://l/logo.png", "https://s/live.m3u8", "News", "France")

        assertEquals(
            Channel("7", "France 24", "https://l/logo.png", "https://s/live.m3u8", "News", "France"),
            dto.toDomain()
        )
    }

    @Test
    fun channelDto_mapsCategoryImageUrlToDomainAndCategory() {
        val dto = ChannelDto(
            id = "7",
            name = "France 24",
            streamUrl = "https://s/live.m3u8",
            category = "News",
            imageUrl = " https://images/news.jpg "
        )

        val channel = dto.toDomain()!!

        assertEquals("https://images/news.jpg", channel.categoryImageUrl)
        assertEquals("https://images/news.jpg", listOf(channel).toCategories().single().imageUrl)
    }

    @Test
    fun channelDto_acceptsHttpImageUrl() {
        val channel = ChannelDto(
            id = "7",
            name = "News",
            streamUrl = "https://s/live.m3u8",
            category = "News",
            imageUrl = "http://images.example.com/news.jpg"
        ).toDomain()!!

        assertEquals("http://images.example.com/news.jpg", channel.categoryImageUrl)
    }

    @Test
    fun channelDto_missingOrNullImageUrl_mapsToNull() {
        val withoutImage = ChannelDto(
            id = "news-1",
            name = "News",
            streamUrl = "https://s/live.m3u8",
            category = "News"
        ).toDomain()!!
        val withNullImage = ChannelDto(
            id = "news-2",
            name = "News",
            streamUrl = "https://s/live-2.m3u8",
            category = "News",
            imageUrl = null
        ).toDomain()!!

        assertNull(withoutImage.categoryImageUrl)
        assertNull(withNullImage.categoryImageUrl)
        assertNull(listOf(withoutImage).toCategories().single().imageUrl)
    }

    @Test
    fun channelDto_rejectsNonHttpImageSchemes() {
        val invalidSchemes = listOf("file:///tmp/news.jpg", "javascript:alert(1)", "//images/news.jpg")

        invalidSchemes.forEach { invalidUrl ->
            val channel = ChannelDto(
                id = "news-${invalidUrl.hashCode()}",
                name = "News",
                streamUrl = "https://s/$invalidUrl.m3u8",
                category = "News",
                imageUrl = invalidUrl
            ).toDomain()!!

            assertNull("Schéma accepté à tort : $invalidUrl", channel.categoryImageUrl)
        }
    }

    @Test
    fun channelDto_withoutNameOrStreamUrl_isRejected() {
        assertNull(ChannelDto(id = "1", name = null, streamUrl = "https://s").toDomain())
        assertNull(ChannelDto(id = "1", name = "X", streamUrl = " ").toDomain())
    }

    @Test
    fun channelDto_appliesSafeDefaultsForMissingOptionalFields() {
        val channel = ChannelDto(id = "x", name = "X", streamUrl = "https://s/x.m3u8").toDomain()!!

        assertEquals("x", channel.id)
        assertEquals("", channel.logoUrl)
        assertEquals(DEFAULT_CATEGORY_NAME, channel.category)
        assertNull(channel.country)
    }

    @Test
    fun channelDtoList_dropsInvalidEntriesAndGroupsByCategory() {
        val dtos = listOf(
            ChannelDto("1", "A", "", "https://s/a", "News", null, "https://images/news.jpg"),
            ChannelDto("2", null, "", "https://s/b", "News"),
            ChannelDto("3", "C", "", "https://s/c", "Sports"),
            ChannelDto("4", "D", "", "https://s/d", "News")
        )

        val categories = dtos.toDomain().toCategories()

        assertEquals(listOf("News", "Sports"), categories.map { it.name })
        assertEquals(listOf("1", "4"), categories[0].channels.map { it.id })
        assertEquals("Sports", categories[1].name)
    }

    @Test
    fun matchItemDto_mapsFieldsAndFiltersInvalidSources() {
        val dto = MatchItemDto(
            id = "m1",
            teamA = "A",
            teamB = "B",
            time = "21:00",
            streamUrl = "https://s/m",
            sources = listOf(
                StreamSourceDto("HD", "https://s/hd"),
                StreamSourceDto("SD", null),
                StreamSourceDto(null, " ")
            )
        )

        val match = dto.toDomain()

        assertEquals("m1", match.id)
        assertEquals("21:00", match.time)
        assertEquals(listOf(StreamSource("HD", "https://s/hd")), match.sources)
    }

    @Test
    fun matchItemDto_withoutSources_keepsNullSources() {
        assertNull(MatchItemDto(id = "m2").toDomain().sources)
    }

    @Test
    fun gson_parsesRemoteJsonIntoDtos_includingMissingFields() {
        val json = """[{"id":"1","name":"A","streamUrl":"https://s/a","category":"News"},{"name":"B"}]"""

        val dtos = Gson().fromJson(json, Array<ChannelDto>::class.java).toList()

        assertEquals(2, dtos.size)
        assertEquals("News", dtos[0].category)
        assertNull(dtos[1].streamUrl)
        assertEquals(1, dtos.toDomain().size)
    }

    @Test
    fun gson_parsesHierarchicalCatalog_andMapsMultiTaggingInDeclaredOrder() {
        val json = """
            {
              "categories": [
                {"name":"Arabe","imageUrl":null},
                {"name":"Arabe/Qatar","imageUrl":"https://images/qatar.jpg"},
                {"name":"News","imageUrl":"javascript:bad"}
              ],
              "channels": [
                {"id":"shared","name":"Shared","streamUrl":"https://s/shared","categories":["Arabe/Qatar","News"]},
                {"id":"invalid","name":"Ignored","streamUrl":null,"categories":["News"]}
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, ChannelsCatalogResponseDto::class.java)
        val mapped = response.toCategories()

        assertEquals(listOf("Arabe", "Arabe/Qatar", "News"), mapped.map { it.name })
        assertEquals(listOf("shared"), mapped[1].channels.map { it.id })
        assertEquals(listOf("shared"), mapped[2].channels.map { it.id })
        assertEquals("https://images/qatar.jpg", mapped[1].imageUrl)
        assertNull(mapped[2].imageUrl)
        assertEquals(0, mapped[0].channelCount)
    }

    @Test
    fun channelDto_requiresIdNameAndStreamUrl() {
        assertNull(ChannelDto(name = "X", streamUrl = "https://s/x").toDomain())
        assertNull(ChannelDto(id = " ", name = "X", streamUrl = "https://s/x").toDomain())
        assertNull(ChannelDto(id = "x", name = "X", streamUrl = null).toDomain())
    }

    @Test
    fun channelDto_rejectsNonHttpStreamUrls() {
        listOf("rtmp://host/live", "javascript:alert(1)", "host/live.m3u8", "//host/live.m3u8")
            .forEach { url ->
                assertNull("Flux accepté à tort : $url", ChannelDto("1", "X", "", url).toDomain())
            }
    }

    @Test
    fun channelDto_sanitizesInvalidLogoUrlToEmpty() {
        val invalid = ChannelDto("1", "X", "javascript:bad", "https://s/x.m3u8").toDomain()!!
        val valid = ChannelDto("2", "X", " https://l/logo.png ", "https://s/x.m3u8").toDomain()!!

        assertEquals("", invalid.logoUrl)
        assertEquals("https://l/logo.png", valid.logoUrl)
    }

    @Test
    fun matchItemDto_sanitizesInvalidUrlsToNullAndKeepsMatch() {
        val match = MatchItemDto(
            id = "m",
            teamALogo = "file:///a.png",
            teamBLogo = "logo.png",
            streamUrl = "rtmp://s/live",
            sources = listOf(
                StreamSourceDto("HD", " https://s/hd "),
                StreamSourceDto("SD", "rtmp://s/sd"),
                StreamSourceDto("LD", "s/ld")
            )
        ).toDomain()

        assertEquals("m", match.id)
        assertNull(match.teamALogo)
        assertNull(match.teamBLogo)
        assertNull(match.streamUrl)
        assertEquals(listOf(StreamSource("HD", "https://s/hd")), match.sources)
    }

    @Test
    fun matchItemDtoList_deduplicatesByNonBlankIdKeepingFirst() {
        val matches = listOf(
            MatchItemDto(id = "a", teamA = "first"),
            MatchItemDto(id = "a", teamA = "second"),
            MatchItemDto(id = null, teamA = "noId1"),
            MatchItemDto(id = " ", teamA = "noId2"),
            MatchItemDto(id = "b", teamA = "third")
        ).toDomain()

        assertEquals(listOf("first", "noId1", "noId2", "third"), matches.map { it.teamA })
    }

    @Test
    fun matchItem_isLiveAt_handlesWindowAndMidnight() {
        fun live(time: String?, h: Int, m: Int) = MatchItem(time = time).isLiveAt(h * 60 + m)

        assertTrue(live("23:00", 23, 30))
        assertTrue(live("23:00", 0, 30))
        assertEquals(false, live("23:00", 1, 0))
        assertTrue(live("00:30", 2, 29))
        assertEquals(false, live("00:30", 2, 30))
        assertEquals(false, live("21:00", 20, 59))
        assertTrue(live("9:00", 9, 0))
        assertEquals(false, MatchItem(time = "21:00").isLiveAt(-1))
        assertEquals(false, MatchItem(time = "21:00").isLiveAt(1440))
    }

    @Test
    fun matchItem_parseStartMinutes_isStrict() {
        assertEquals(540, MatchItem(time = "9:00").parseStartMinutes())
        assertEquals(1260, MatchItem(time = "21:00").parseStartMinutes())
        listOf(null, "", "25:00", "12:60", "12:30:45", "LIVE", "12", "-1:00", "12:5")
            .forEach { assertNull("Heure acceptée à tort : $it", MatchItem(time = it).parseStartMinutes()) }
        assertEquals(false, MatchItem(time = "25:00").isLiveAt(25 * 60 % 1440))
        assertEquals(false, MatchItem(time = "12:30:45").isLiveAt(12 * 60 + 30))
    }

    @Test
    fun gson_parsesMatchJsonWithSources() {
        val json = """[{"id":"m","teamA":"A","sources":[{"quality":"HD","url":"https://s/hd"}]}]"""

        val match = Gson().fromJson(json, Array<MatchItemDto>::class.java).toList().toDomain().single()

        assertEquals(MatchItem(id = "m", teamA = "A", sources = listOf(StreamSource("HD", "https://s/hd"))), match)
    }

    /** Garde-fou : le Domain ne doit porter aucune annotation de sérialisation Gson. */
    @Test
    fun domainModels_carryNoGsonAnnotations() {
        listOf(Channel::class.java, Category::class.java, MatchItem::class.java, StreamSource::class.java)
            .forEach { clazz ->
                val annotated = clazz.declaredFields.filter { field ->
                    field.annotations.any { it.annotationClass.java.name.startsWith("com.google.gson") }
                }
                assertTrue("${clazz.simpleName} contient des annotations Gson : $annotated", annotated.isEmpty())
            }
    }
}

