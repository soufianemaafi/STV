package com.example.soukitv.features.home.data.mapper

import com.example.soukitv.features.home.data.model.ChannelDto
import com.example.soukitv.features.home.data.model.MatchItemDto
import com.example.soukitv.features.home.data.model.StreamSourceDto
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource
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
    fun channelDto_withoutNameOrStreamUrl_isRejected() {
        assertNull(ChannelDto(id = "1", name = null, streamUrl = "https://s").toDomain())
        assertNull(ChannelDto(id = "1", name = "X", streamUrl = " ").toDomain())
    }

    @Test
    fun channelDto_appliesSafeDefaultsForMissingOptionalFields() {
        val channel = ChannelDto(name = "X", streamUrl = "https://s/x.m3u8").toDomain()!!

        assertEquals("https://s/x.m3u8", channel.id)
        assertEquals("", channel.logoUrl)
        assertEquals(DEFAULT_CATEGORY_NAME, channel.category)
        assertNull(channel.country)
    }

    @Test
    fun channelDtoList_dropsInvalidEntriesAndGroupsByCategory() {
        val dtos = listOf(
            ChannelDto("1", "A", "", "https://s/a", "News"),
            ChannelDto("2", null, "", "https://s/b", "News"),
            ChannelDto("3", "C", "", "https://s/c", "Sports"),
            ChannelDto("4", "D", "", "https://s/d", "News")
        )

        val categories = dtos.toDomain().toCategories()

        assertEquals(listOf("News", "Sports"), categories.map { it.name })
        assertEquals(listOf("1", "4"), categories[0].channels.map { it.id })
        assertTrue(categories[1] is Category)
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

