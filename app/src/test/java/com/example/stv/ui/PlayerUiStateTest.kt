package com.example.stv.features.player.presentation

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour PlayerUiState.
 * Vérifie que la state machine a les bons types et propriétés.
 */
class PlayerUiStateTest {

    @Test
    fun `LoadingAds contient la bonne URL`() {
        val state = PlayerUiState.LoadingAds("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `ShowingAd contient la bonne URL`() {
        val state = PlayerUiState.ShowingAd("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `Ready contient la bonne URL`() {
        val state = PlayerUiState.Ready("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `Error contient le bon message`() {
        val state = PlayerUiState.Error("URL not provided")
        assertEquals("URL not provided", state.message)
    }

    @Test
    fun `AdBlockerBlocked contient la bonne URL`() {
        val state = PlayerUiState.AdBlockerBlocked("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `NeedRetry contient la bonne URL`() {
        val state = PlayerUiState.NeedRetry("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `NetworkError contient la bonne URL`() {
        val state = PlayerUiState.NetworkError("https://example.com/stream.m3u8")
        assertEquals("https://example.com/stream.m3u8", state.videoUrl)
    }

    @Test
    fun `les etats sont distincts avec le meme URL`() {
        val url = "https://example.com/stream.m3u8"
        val loading = PlayerUiState.LoadingAds(url)
        val ready = PlayerUiState.Ready(url)
        val showing = PlayerUiState.ShowingAd(url)
        val blocked = PlayerUiState.AdBlockerBlocked(url)

        assertNotEquals(loading, ready)
        assertNotEquals(loading, showing)
        assertNotEquals(ready, showing)
        assertNotEquals(loading, blocked)
        assertNotEquals(ready, blocked)
        assertNotEquals(showing, blocked)
    }

    @Test
    fun `deux LoadingAds avec la meme URL sont egaux`() {
        val a = PlayerUiState.LoadingAds("https://example.com/stream.m3u8")
        val b = PlayerUiState.LoadingAds("https://example.com/stream.m3u8")
        assertEquals(a, b)
    }
}

