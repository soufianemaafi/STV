package com.example.soukitv.data

import com.example.soukitv.model.Category
import com.example.soukitv.model.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChannelRepository {

    // Simulate fetching from a dashboard/API
    fun getCategories(): Flow<List<Category>> = flow {
        // In a real app, you would make a network call here (Retrofit)
        delay(1000) // Simulate network latency

        val testUrl = "https://stream.skynewsarabia.com/hls/sna_720.m3u8"
        val channels = listOf(
            Channel("1", "France 24", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e2/France_24_logo.svg/1200px-France_24_logo.svg.png", testUrl, "News"),
            Channel("2", "Al Jazeera", "https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Al_Jazeera_English_Logo.svg/1200px-Al_Jazeera_English_Logo.svg.png", testUrl, "News"),
            Channel("3", "NASA TV", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/1200px-NASA_logo.svg.png", "https://ntv1.akamaized.net/hls/live/2013975/NASA-NTV1-HLS/master.m3u8", "Science"),
            Channel("4", "Red Bull TV", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/Red_Bull_TV_logo.svg/1200px-Red_Bull_TV_logo.svg.png", "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8", "Sports"),
             Channel("5", "Big Buck Bunny", "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Movies")
        )

        // Group by category
        val categories = channels.groupBy { it.category }
            .map { (categoryName, channelList) ->
                Category(categoryName, channelList)
            }
            // Optional: Sort categories if needed

        emit(categories)
    }
}

