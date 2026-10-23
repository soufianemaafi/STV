package com.stv.videoplayer.features.player.presentation

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.stv.videoplayer.core.domain.model.StreamSource
import com.stv.videoplayer.player.HlsMimeResolver
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // âœ… PlayerViewModel utilise viewModelScope (Dispatchers.Main) pour la coroutine
        // de mise Ã  jour de position â€” indispensable en test JVM pur.
        Dispatchers.setMain(testDispatcher)

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

    // ==================== Headers HTTP : flux par instance via MediaItem ====================

    private val partnerHeaders = mapOf("Referer" to "https://partner.example.com", "X-Token" to "abc")

    private fun capturedMediaItems(expectedCalls: Int): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        verify(exactly = expectedCalls) { mockPlayer.setMediaItem(capture(items)) }
        return items
    }

    @Test
    fun `les headers de LoadVideo sont injectes dans le RequestMetadata du MediaItem`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        viewModel.onAction(
            PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true, headers = partnerHeaders)
        )
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))

        val item = capturedMediaItems(1).single()
        assertNotNull(item.requestMetadata.extras)
    }

    @Test
    fun `sans header le MediaItem ne porte aucun extra de requete`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true))
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))

        assertNull(capturedMediaItems(1).single().requestMetadata.extras)
    }

    @Test
    fun `les headers d'une session ne fuitent pas vers la session suivante`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE

        viewModel.onAction(
            PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true, headers = partnerHeaders)
        )
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/b.m3u8", skipAds = true))
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/b.m3u8"))

        val items = capturedMediaItems(2)
        assertNotNull(items[0].requestMetadata.extras)
        assertNull(items[1].requestMetadata.extras)
    }

    @Test
    fun `deux ViewModels ne partagent aucun header`() {
        val otherPlayer = mockk<Player>(relaxed = true)
        every { otherPlayer.playbackState } returns Player.STATE_IDLE
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        val otherViewModel = PlayerViewModel(application) { otherPlayer }

        viewModel.onAction(
            PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true, headers = partnerHeaders)
        )
        otherViewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))

        val otherItems = mutableListOf<MediaItem>()
        verify(exactly = 1) { otherPlayer.setMediaItem(capture(otherItems)) }
        assertNull(otherItems.single().requestMetadata.extras)
    }

    @Test
    fun `la meme URL avec des headers differents est repreparee`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        viewModel.onAction(
            PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true, headers = partnerHeaders)
        )
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))

        every { mockPlayer.playbackState } returns Player.STATE_READY
        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                "https://example.com/a.m3u8",
                skipAds = true,
                headers = mapOf("X-Token" to "other")
            )
        )
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))

        capturedMediaItems(2)
    }

    @Test
    fun `SwitchSource conserve les headers de la session`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        val sources = listOf(
            StreamSource("HD", "https://example.com/hd.m3u8"),
            StreamSource("SD", "https://example.com/sd.m3u8")
        )
        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                "https://example.com/hd.m3u8",
                skipAds = true,
                headers = partnerHeaders,
                sources = sources
            )
        )
        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/hd.m3u8"))

        viewModel.onAction(PlayerUiAction.SwitchSource("https://example.com/sd.m3u8", 1))

        val items = capturedMediaItems(2)
        assertNotNull(items[1].requestMetadata.extras)
    }

    @Test
    fun `les retries epuises basculent automatiquement de la source 0 a la source 1`() {
        val sources = listOf(
            StreamSource("Primaire", "https://example.com/primary.m3u8"),
            StreamSource("Secours", "https://example.com/backup.m3u8")
        )
        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                videoUrl = sources[0].url,
                skipAds = true,
                sources = sources
            )
        )
        val error = PlaybackException(
            "network",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        // Trois retries sont autorisés ; l'erreur suivante déclenche le failover.
        repeat(4) { listenerSlot.captured.onPlayerError(error) }

        assertEquals(1, viewModel.currentSourceIndex.value)
        assertEquals(
            PlayerUiState.Ready(sources[1].url, currentSourceIndex = 1),
            viewModel.uiState.value
        )
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `une erreur de manifest malforme bascule de la source 0 a la source 1`() {
        val sources = listOf(
            StreamSource("Primaire", "https://example.com/primary.m3u8"),
            StreamSource("Secours", "https://example.com/backup.m3u8")
        )
        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                videoUrl = sources[0].url,
                skipAds = true,
                sources = sources
            )
        )

        listenerSlot.captured.onPlayerError(
            PlaybackException(
                "malformed manifest",
                null,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
            )
        )

        assertEquals(1, viewModel.currentSourceIndex.value)
        assertEquals(
            PlayerUiState.Ready(sources[1].url, currentSourceIndex = 1),
            viewModel.uiState.value
        )
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `l'erreur finale est affichee quand la derniere source echoue`() {
        val sources = listOf(
            StreamSource("Primaire", "https://example.com/primary.m3u8"),
            StreamSource("Secours", "https://example.com/backup.m3u8")
        )
        viewModel.onAction(
            PlayerUiAction.LoadVideo(
                videoUrl = sources[0].url,
                skipAds = true,
                sources = sources
            )
        )
        viewModel.onAction(PlayerUiAction.SwitchSource(sources[1].url, qualityIndex = 1))
        val error = PlaybackException(
            "network",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        repeat(4) { listenerSlot.captured.onPlayerError(error) }

        assertEquals(1, viewModel.currentSourceIndex.value)
        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `aucun etat statique de headers n'existe plus dans PlayerViewModel`() {
        val staticHeaderFields = PlayerViewModel::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .filter { it.name.contains("header", ignoreCase = true) }

        assertTrue("Etat statique de headers detecte : $staticHeaderFields", staticHeaderFields.isEmpty())
    }

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
    fun `un Pure Live non dynamique masque les controles seekables`() {
        every { mockPlayer.isCurrentMediaItemLive } returns true
        every { mockPlayer.isCurrentMediaItemDynamic } returns false

        listenerSlot.captured.onTimelineChanged(mockk(relaxed = true), 0)

        assertFalse(viewModel.isSeekable.value)
    }

    @Test
    fun `un Live DVR dynamique conserve les controles seekables`() {
        every { mockPlayer.isCurrentMediaItemLive } returns true
        every { mockPlayer.isCurrentMediaItemDynamic } returns true

        listenerSlot.captured.onMediaItemTransition(null, 0)

        assertTrue(viewModel.isSeekable.value)
    }

    @Test
    fun `une VOD reste seekable`() {
        every { mockPlayer.isCurrentMediaItemLive } returns false
        every { mockPlayer.isCurrentMediaItemDynamic } returns false

        listenerSlot.captured.onTimelineChanged(mockk(relaxed = true), 0)

        assertTrue(viewModel.isSeekable.value)
    }

    @Test
    fun `onPlayerError renseigne errorMessage et desactive isLoading`() {
        // âœ… `PlaybackException.errorCode` est un champ Java final (pas une mÃ©thode
        // virtuelle) : on ne peut pas le mocker via `every {}` â€” on construit donc
        // une vraie instance (aucune dÃ©pendance Android runtime nÃ©cessaire ici).
        val error = PlaybackException(
            "bad status",
            null,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        )

        listenerSlot.captured.onPlayerError(error)

        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.errorMessage.value)
    }

    // ==================== Lot critique : zapping propre (F1) ====================

    @Test
    fun `LoadVideo coupe immediatement le flux precedent`() {
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true))
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/b.m3u8", skipAds = true))

        // 1 appel par LoadVideo (A puis B) : le flux de A est coupe avant B.
        verify(exactly = 2) { mockPlayer.stop() }
        verify(exactly = 2) { mockPlayer.clearMediaItems() }
    }

    @Test
    fun `une PreparePlayback perimee d'une ancienne chaine est ignoree et n'herite pas des headers`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        viewModel.onAction(
            PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true, headers = partnerHeaders)
        )
        // Zapping ultra-rapide : B arrive avant que PreparePlayback(A) ne soit traitee.
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/b.m3u8", skipAds = true))

        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/a.m3u8"))
        verify(exactly = 0) { mockPlayer.setMediaItem(any<MediaItem>()) }

        viewModel.onAction(PlayerUiAction.PreparePlayback("https://example.com/b.m3u8"))
        assertNull(capturedMediaItems(1).single().requestMetadata.extras)
    }

    @Test
    fun `PreparePlayback avec headers explicites utilise exactement ces headers`() {
        every { mockPlayer.playbackState } returns Player.STATE_IDLE
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/b.m3u8", skipAds = true))

        viewModel.onAction(
            PlayerUiAction.PreparePlayback("https://example.com/b.m3u8", headers = partnerHeaders)
        )

        assertNotNull(capturedMediaItems(1).single().requestMetadata.extras)
    }

    @Test
    fun `la re-emission du meme Intent ne coupe pas un flux en cours`() {
        every { mockPlayer.playbackState } returns Player.STATE_READY
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true))
        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/a.m3u8", skipAds = true))

        verify(exactly = 1) { mockPlayer.stop() }
    }

    // ==================== Lot critique : auto-recovery (F2) ====================

    @Test
    fun `BEHIND_LIVE_WINDOW rattrape le direct sans afficher d'erreur`() {
        val error = PlaybackException(
            "behind live window",
            null,
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
        )

        listenerSlot.captured.onPlayerError(error)

        verify(exactly = 1) { mockPlayer.seekToDefaultPosition() }
        verify(exactly = 1) { mockPlayer.prepare() }
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `une erreur reseau est reessayee avec backoff exponentiel puis devient definitive apres 3 essais`() {
        val error = PlaybackException(
            "network",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        // Essai 1 : backoff 1500 ms
        listenerSlot.captured.onPlayerError(error)
        assertNull(viewModel.errorMessage.value)
        verify(exactly = 0) { mockPlayer.prepare() }
        testDispatcher.scheduler.advanceTimeBy(1_500L); testDispatcher.scheduler.runCurrent()
        verify(exactly = 1) { mockPlayer.prepare() }

        // Essai 2 : backoff 3000 ms
        listenerSlot.captured.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(2_999L); testDispatcher.scheduler.runCurrent()
        verify(exactly = 1) { mockPlayer.prepare() }
        testDispatcher.scheduler.advanceTimeBy(1L); testDispatcher.scheduler.runCurrent()
        verify(exactly = 2) { mockPlayer.prepare() }

        // Essai 3 : backoff 4500 ms
        listenerSlot.captured.onPlayerError(error)
        testDispatcher.scheduler.advanceTimeBy(4_500L); testDispatcher.scheduler.runCurrent()
        verify(exactly = 3) { mockPlayer.prepare() }

        // 4e erreur : retryCount >= 3 -> erreur definitive
        listenerSlot.captured.onPlayerError(error)
        assertNotNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `STATE_READY remet le compteur de retry a zero`() {
        every { mockPlayer.duration } returns 1_000L
        val error = PlaybackException(
            "network",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )

        repeat(3) {
            listenerSlot.captured.onPlayerError(error)
        }
        listenerSlot.captured.onPlaybackStateChanged(Player.STATE_READY)

        listenerSlot.captured.onPlayerError(error)
        assertNull("Le compteur doit etre reparti de 0", viewModel.errorMessage.value)
    }

    @Test
    fun `un nouveau LoadVideo annule le retry reseau en attente`() {
        val error = PlaybackException(
            "network",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        listenerSlot.captured.onPlayerError(error)

        viewModel.onAction(PlayerUiAction.LoadVideo("https://example.com/b.m3u8", skipAds = true))
        testDispatcher.scheduler.advanceTimeBy(10_000L); testDispatcher.scheduler.runCurrent()

        verify(exactly = 0) { mockPlayer.prepare() }
    }

    // ==================== Lot critique : MIME HLS ====================

    @Test
    fun `HlsMimeResolver force le MIME HLS pour une URL m3u8 avec query et fragment`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            HlsMimeResolver.resolve("https://example.com/live.M3U8?token=abc#x")
        )
    }

    @Test
    fun `HlsMimeResolver applique le MIME HLS de l'Intent meme sans extension`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            HlsMimeResolver.resolve("https://example.com/live?id=1", "application/vnd.apple.mpegurl; charset=utf-8")
        )
    }

    @Test
    fun `HlsMimeResolver ne force rien pour un mp4`() {
        assertNull(HlsMimeResolver.resolve("https://example.com/video.mp4?x=.m3u8"))
        assertNull(HlsMimeResolver.resolve("https://example.com/video.mp4", "video/mp4"))
    }

    // ==================== Lot critique : pause en arriere-plan (F4) ====================

    @Test
    fun `Background met en pause hors Picture-in-Picture`() {
        viewModel.onAction(PlayerUiAction.Background(isInPictureInPicture = false))
        verify(exactly = 1) { mockPlayer.pause() }
    }

    @Test
    fun `Background ne met pas en pause en Picture-in-Picture`() {
        viewModel.onAction(PlayerUiAction.Background(isInPictureInPicture = true))
        verify(exactly = 0) { mockPlayer.pause() }
    }

    @Test
    fun `Foreground reprend uniquement si la lecture etait active`() {
        every { mockPlayer.playWhenReady } returns true
        viewModel.onAction(PlayerUiAction.Background(isInPictureInPicture = false))
        viewModel.onAction(PlayerUiAction.Foreground)
        verify(exactly = 1) { mockPlayer.play() }

        every { mockPlayer.playWhenReady } returns false
        viewModel.onAction(PlayerUiAction.Background(isInPictureInPicture = false))
        viewModel.onAction(PlayerUiAction.Foreground)
        verify(exactly = 1) { mockPlayer.play() }
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



