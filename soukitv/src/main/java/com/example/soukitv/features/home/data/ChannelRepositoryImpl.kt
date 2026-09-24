package com.example.soukitv.features.home.data

import android.util.Log
import com.example.soukitv.features.home.data.remote.ChannelApiService
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChannelRepositoryImpl : ChannelRepository {

    private val apiService: ChannelApiService = Retrofit.Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ChannelApiService::class.java)

    override fun getCategories(): Flow<List<Category>> = flow {
        val categories = try {
            withContext(Dispatchers.IO) {
                apiService.getChannels(REMOTE_CHANNELS_URL)
            }.groupBy { it.category }
                .map { (categoryName, channelList) ->
                    Category(categoryName, channelList)
                }
        } catch (throwable: Exception) {
            Log.w(TAG, "Remote catalogue unavailable, falling back to local channels", throwable)
            localCategories()
        }

        emit(categories)
    }

    private fun localCategories(): List<Category> {
        val testUrl = "https://stream.skynewsarabia.com/hls/sna_720.m3u8"
        val channels = listOf(
            Channel("1", "France 24", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/france-24-fr.png", testUrl, "News"),
            Channel("2", "Al Jazeera", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/al-jazeera-english.png", testUrl, "News"),
            Channel("3", "NASA TV", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/nasa-tv.png", "https://ntv1.akamaized.net/hls/live/2013975/NASA-NTV1-HLS/master.m3u8", "Science"),
            Channel("4", "Red Bull TV", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/red-bull-tv.png", "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8", "Sports"),
            Channel("5", "Big Buck Bunny", "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Movies"),
            Channel("6", "BFM TV", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TV/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("7", "BFM Business", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-business-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_BUSINESS/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("8", "BFM 2", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM2/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("9", "BFM Tech & Co", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TECHANDCO/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("10", "BFM Grands Reportages", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_GRANDSREPORTAGES/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("11", "BFM Lyon", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-lyon-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_LYON/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("12", "BFM Marseille", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-marseille-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_MARSEILLEPROV/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("13", "BFM Nice Côte d'Azur", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-nice-cote-d-azur-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_NICECOTEDAZUR/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("14", "BFM Alsace", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_ALSACE/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("15", "BFM Normandie", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_NORMANDIE/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("16", "BFM Toulon Var", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_TOULONVAR/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("17", "BFM Grand Lille", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFMGRANDLILLE/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("18", "BFM Grand Littoral", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFMGRANDLITTORAL/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("19", "BFM DICI Alpes du Sud", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_DICI_ALPESDUSUD/index.m3u8?end=END&start=LIVE", "BFM News & Régions"),
            Channel("20", "BFM DICI Haute-Provence", "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/france/bfm-tv-fr.png", "https://ncdn-live-bfm.pfd.sfr.net/shls/LIVE${'$'}BFM_DICI_HAUTEPROVENCE/index.m3u8?end=END&start=LIVE", "BFM News & Régions")
        )

        return channels.groupBy { it.category }
            .map { (categoryName, channelList) ->
                Category(categoryName, channelList)
            }
    }

    companion object {
        private const val TAG = "ChannelRepositoryImpl"
        private const val REMOTE_CHANNELS_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/channels.json"
    }
}

