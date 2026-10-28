package com.example.soukitv.features.home.data

import android.util.Log
import com.example.soukitv.features.home.data.mapper.toCategories
import com.example.soukitv.features.home.data.mapper.toDomain
import com.example.soukitv.features.home.data.remote.ChannelApiService
import com.example.soukitv.features.home.domain.model.Category
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
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
            }.toDomain()
                .toCategories()
                .ifEmpty { localCategories() }
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

        return channels.toCategories()
    }

    /**
     * Matchs du jour : récupérés depuis [REMOTE_MATCHES_URL] sur Dispatchers.IO.
     * En cas d'échec réseau, bascule sur une liste de secours locale.
     */
    override fun getMatches(): Flow<List<MatchItem>> = flow {
        val matches = try {
            withContext(Dispatchers.IO) {
                apiService.getMatches(REMOTE_MATCHES_URL)
            }.toDomain()
        } catch (throwable: Exception) {
            Log.w(TAG, "Remote matches unavailable, falling back to local matches", throwable)
            fallbackMatches()
        }
        emit(matches)
    }

    /** Matchs de secours (hors-ligne), libelles en arabe. */
    private fun fallbackMatches(): List<MatchItem> {
        val fallbackStream = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        return listOf(
            MatchItem(
                id = "match-1",
                teamA = "ريال مدريد",
                teamALogo = "https://upload.wikimedia.org/wikipedia/en/thumb/5/56/Real_Madrid_CF.svg/200px-Real_Madrid_CF.svg.png",
                teamB = "برشلونة",
                teamBLogo = "https://upload.wikimedia.org/wikipedia/en/thumb/4/47/FC_Barcelona_%28crest%29.svg/200px-FC_Barcelona_%28crest%29.svg.png",
                time = "21:00",
                tournament = "دوري أبطال أوروبا",
                channelName = "STV Sports 1",
                streamUrl = fallbackStream
            ),
            MatchItem(
                id = "match-2",
                teamA = "مانشستر سيتي",
                teamALogo = "https://upload.wikimedia.org/wikipedia/en/thumb/e/eb/Manchester_City_FC_badge.svg/200px-Manchester_City_FC_badge.svg.png",
                teamB = "ليفربول",
                teamBLogo = "https://upload.wikimedia.org/wikipedia/en/thumb/0/0c/Liverpool_FC.svg/200px-Liverpool_FC.svg.png",
                time = "18:30",
                tournament = "الدوري الإنجليزي الممتاز",
                channelName = "STV Sports 2",
                streamUrl = fallbackStream
            ),
            MatchItem(
                id = "match-3",
                teamA = "الوداد الرياضي",
                teamALogo = "https://upload.wikimedia.org/wikipedia/en/thumb/2/2e/Wydad_AC_logo.svg/200px-Wydad_AC_logo.svg.png",
                teamB = "الرجاء الرياضي",
                teamBLogo = "https://upload.wikimedia.org/wikipedia/en/thumb/5/5d/Raja_CA_Logo.svg/200px-Raja_CA_Logo.svg.png",
                time = "20:00",
                tournament = "البطولة الاحترافية إنوي",
                channelName = "STV Sports 3",
                streamUrl = fallbackStream
            )
        )
    }

    companion object {
        private const val TAG = "ChannelRepositoryImpl"
        private const val REMOTE_CHANNELS_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/channels.json"
        private const val REMOTE_MATCHES_URL = "https://raw.githubusercontent.com/elcodefy/souki-chanels/refs/heads/main/matches.json"
    }
}

