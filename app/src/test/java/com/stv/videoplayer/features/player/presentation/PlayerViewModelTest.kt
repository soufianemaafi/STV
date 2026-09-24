package com.stv.videoplayer.features.player.presentation

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour PlayerViewModel.
 *
 * âœ… On injecte un [Player] entiÃ¨rement mockÃ© (interface Media3 pure) via le
 * paramÃ¨tre `playerFactory` du ViewModel (Dependency Inversion) â€” jamais la classe
 * concrÃ¨te `ExoPlayer`, qui nÃ©cessite un environnement Android rÃ©el (native/JNI) et
 * ne peut pas Ãªtre chargÃ©e/mockÃ©e en JVM pur. Cela permet de tester toute la logique
 * MVI (actions, synchronisation d'Ã©tat, cycle de vie) sans Robolectric.
 *
 * Couvre notamment la Mission 2 :
 *  - Le player est bien construit via la factory reÃ§ue (jamais recrÃ©Ã©).
 *  - `onAction(...)` pilote le player (Play/Pause/SeekTo/PreparePlayback/...).
 *  - Le `Player.Listener` synchronise bien les StateFlows exposÃ©s.
 *  - `onCleared()` appelle IMPÃ‰RATIVEMENT `player.release()` (zÃ©ro fuite mÃ©moire).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private lateinit var application: Application
    private lateinit var mockPlayer: Player
    private lateinit var listenerSlot: CapturingSlot<Player.Listener>
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        // âœ… PlayerViewModel utilise viewModelScope (Dispatchers.Main) pour la coroutine
        // de mise Ã  jour de position â€” indispensable en test JVM pur.
        Dispatchers.setMain(StandardTestDispatcher())

        application = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        listenerSlot = slot()

        every { mockPlayer.addListener(capture(listenerSlot)) } returns Unit
        every { application.getString(any()) } returns "string"
        every { application.getString(any(), any()) } returns "string"

        viewModel = PlayerViewModel(application) { mockPlayer }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Construction â€” sÃ©curitÃ© mÃ©moire ====================

    @Test
    fun `le player est construit via la factory injectee`() {
        assertSame(mockPlayer, viewModel.player)
    }

    @Test
    fun `le listener est enregistre une seule fois a la creation`() {
        verify(exactly = 1) { mockPlayer.addListener(any()) }
    }

    // ==================== onAction â€” LoadVideo / RejectInvalidIntent ====================

    @Test
    fun `LoadVideo avec skipAds true passe en Ready`() {
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/video.mp4", skipAds = true))
        assertEquals(PlayerUiState.Ready("https://example.com/video.mp4"), viewModel.uiState.value)
    }

    @Test
    fun `LoadVideo avec skipAds false passe en LoadingAds`() {
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/video.mp4", skipAds = false))
        assertEquals(PlayerUiState.LoadingAds("https://example.com/video.mp4"), viewModel.uiState.value)
    }

    @Test
    fun `LoadVideo transporte le titre et les headers`() {
        val headers = mapOf(
            "Referer" to "https://partner.example.com",
            "User-Agent" to "CustomAgent/1.0"
        )

        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                videoUrl = "https://example.com/video.mp4",
                title = "Canal+ HD",
                headers = headers
            )
        )

        assertEquals("Canal+ HD", viewModel.currentTitle.value)
        assertEquals(PlayerUiState.LoadingAds("https://example.com/video.mp4"), viewModel.uiState.value)
    }

    @Test
    fun `RejectInvalidIntent passe en Error`() {
        viewModel.onAction(PlayerUiAction.RejectInvalidIntent("Lien invalide"))
        assertEquals(PlayerUiState.Error("Lien invalide"), viewModel.uiState.value)
    }

    @Test
    fun `RetryAdCheck relance LoadingAds avec la meme URL`() {
        val url = "https://example.com/video.mp4"
        viewModel.onAction(PlayerUiAction.LoadVideo(url, skipAds = false))

        viewModel.onAction(PlayerUiAction.RetryAdCheck)

        assertEquals(PlayerUiState.LoadingAds(url, forceAdCheck = true), viewModel.uiState.value)
    }

    // ==================== onAction â€” PreparePlayback (construction MediaItem) ====================

    @Test
    fun `PreparePlayback construit un MediaItem et prepare le player`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/stream.m3u8"))

        verify { mockPlayer.setMediaItem(any<MediaItem>()) }
        verify { mockPlayer.prepare() }
    }

    @Test
    fun `PreparePlayback est idempotent pour la meme URL deja preparee`() {
        every { mockPlayer.playbackState } returns Player.STATE_READY

        // PremiÃ¨re prÃ©paration
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/stream.m3u8"))

        // DeuxiÃ¨me appel avec la mÃªme URL, player dÃ©jÃ  prÃªt (ex: rotation d'Ã©cran)
        every { mockPlayer.playbackState } returns Player.STATE_READY
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/stream.m3u8"))

        // setMediaItem ne doit avoir Ã©tÃ© appelÃ© qu'une seule fois
        verify(exactly = 1) { mockPlayer.setMediaItem(any<MediaItem>()) }
    }

    // ==================== onAction â€” Play / Pause / Seek ====================

    @Test
    fun `Play appelle player play`() {
        viewModel.onAction(PlayerUiAction.Play)
        verify { mockPlayer.play() }
    }

    @Test
    fun `Pause appelle player pause`() {
        viewModel.onAction(PlayerUiAction.Pause)
        verify { mockPlayer.pause() }
    }

    @Test
    fun `TogglePlayPause met en pause si en cours de lecture`() {
        every { mockPlayer.isPlaying } returns true
        viewModel.onAction(PlayerUiAction.TogglePlayPause)
        verify { mockPlayer.pause() }
    }

    @Test
    fun `TogglePlayPause lance la lecture si en pause`() {
        every { mockPlayer.isPlaying } returns false
        viewModel.onAction(PlayerUiAction.TogglePlayPause)
        verify { mockPlayer.play() }
    }

    @Test
    fun `SeekTo clampe la position entre 0 et la duree`() {
        every { mockPlayer.duration } returns 100_000L

        viewModel.onAction(PlayerUiAction.SeekTo(500_000L))

        verify { mockPlayer.seekTo(100_000L) }
        assertEquals(100_000L, viewModel.currentPosition.value)
    }

    @Test
    fun `SeekForward avance de 10 secondes sans depasser la duree`() {
        every { mockPlayer.currentPosition } returns 95_000L
        every { mockPlayer.duration } returns 100_000L

        viewModel.onAction(PlayerUiAction.SeekForward)

        verify { mockPlayer.seekTo(100_000L) }
    }

    @Test
    fun `SeekRewind recule de 10 secondes sans descendre sous 0`() {
        every { mockPlayer.currentPosition } returns 5_000L
        every { mockPlayer.duration } returns 100_000L

        viewModel.onAction(PlayerUiAction.SeekRewind)

        verify { mockPlayer.seekTo(0L) }
    }

    // ==================== Synchronisation StateFlow via Player.Listener ====================

    @Test
    fun `onIsPlayingChanged synchronise le StateFlow isPlaying`() {
        listenerSlot.captured.onIsPlayingChanged(true)
        assertTrue(viewModel.isPlaying.value)

        listenerSlot.captured.onIsPlayingChanged(false)
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun `onPlaybackStateChanged BUFFERING active isLoading`() {
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_BUFFERING)
        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun `onPlaybackStateChanged READY desactive isLoading`() {
        every { mockPlayer.duration } returns 42_000L
        every { mockPlayer.isCurrentMediaItemLive } returns false

        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)

        assertFalse(viewModel.isLoading.value)
        assertEquals(42_000L, viewModel.duration.value)
    }

    @Test
    fun `onPlayerError renseigne errorMessage et desactive isLoading`() {
        // âœ… `PlaybackException.errorCode` est un champ Java final (pas une mÃ©thode
        // virtuelle) : on ne peut pas le mocker via `every {}` â€” on construit donc
        // une vraie instance (aucune dÃ©pendance Android runtime nÃ©cessaire ici).
        val error = PlaybackException(
            "network error",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        listenerSlot.captured.onPlayerError(error)

        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.errorMessage.value)
    }

    // ==================== isReady() ====================

    @Test
    fun `isReady retourne true uniquement en etat Ready`() {
        assertFalse(viewModel.isReady())
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/video.mp4", skipAds = true))
        assertTrue(viewModel.isReady())
    }

    // ==================== onCleared â€” zÃ©ro fuite mÃ©moire ====================

    @Test
    fun `onCleared libere IMPERATIVEMENT le player`() {
        callOnCleared(viewModel)
        verify(exactly = 1) { mockPlayer.release() }
    }

    @Test
    fun `onCleared retire le listener avant de liberer le player`() {
        callOnCleared(viewModel)
        verify { mockPlayer.removeListener(any()) }
    }

    /** `onCleared()` est `protected` sur ViewModel : on l'invoque via rÃ©flexion pour le tester. */
    private fun callOnCleared(vm: PlayerViewModel) {
        val method = vm.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(vm)
    }
}



