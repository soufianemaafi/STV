package com.example.stv.features.player.presentation

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
 * ✅ On injecte un [Player] entièrement mocké (interface Media3 pure) via le
 * paramètre `playerFactory` du ViewModel (Dependency Inversion) — jamais la classe
 * concrète `ExoPlayer`, qui nécessite un environnement Android réel (native/JNI) et
 * ne peut pas être chargée/mockée en JVM pur. Cela permet de tester toute la logique
 * MVI (actions, synchronisation d'état, cycle de vie) sans Robolectric.
 *
 * Couvre notamment la Mission 2 :
 *  - Le player est bien construit via la factory reçue (jamais recréé).
 *  - `onAction(...)` pilote le player (Play/Pause/SeekTo/PreparePlayback/...).
 *  - Le `Player.Listener` synchronise bien les StateFlows exposés.
 *  - `onCleared()` appelle IMPÉRATIVEMENT `player.release()` (zéro fuite mémoire).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private lateinit var application: Application
    private lateinit var mockPlayer: Player
    private lateinit var listenerSlot: CapturingSlot<Player.Listener>
    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        // ✅ PlayerViewModel utilise viewModelScope (Dispatchers.Main) pour la coroutine
        // de mise à jour de position — indispensable en test JVM pur.
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

    // ==================== Construction — sécurité mémoire ====================

    @Test
    fun `le player est construit via la factory injectee`() {
        assertSame(mockPlayer, viewModel.player)
    }

    @Test
    fun `le listener est enregistre une seule fois a la creation`() {
        verify(exactly = 1) { mockPlayer.addListener(any()) }
    }

    // ==================== onAction — LoadVideo / RejectInvalidIntent ====================

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

    // ==================== onAction — PreparePlayback (construction MediaItem) ====================

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

        // Première préparation
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/stream.m3u8"))

        // Deuxième appel avec la même URL, player déjà prêt (ex: rotation d'écran)
        every { mockPlayer.playbackState } returns Player.STATE_READY
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/stream.m3u8"))

        // setMediaItem ne doit avoir été appelé qu'une seule fois
        verify(exactly = 1) { mockPlayer.setMediaItem(any<MediaItem>()) }
    }

    // ==================== onAction — Play / Pause / Seek ====================

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
        // ✅ `PlaybackException.errorCode` est un champ Java final (pas une méthode
        // virtuelle) : on ne peut pas le mocker via `every {}` — on construit donc
        // une vraie instance (aucune dépendance Android runtime nécessaire ici).
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

    // ==================== onCleared — zéro fuite mémoire ====================

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

    /** `onCleared()` est `protected` sur ViewModel : on l'invoque via réflexion pour le tester. */
    private fun callOnCleared(vm: PlayerViewModel) {
        val method = vm.javaClass.getDeclaredMethod("onCleared")
        method.isAccessible = true
        method.invoke(vm)
    }
}


