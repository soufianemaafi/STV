package com.stv.videoplayer.features.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFrequencyManagerTest {

    @Test
    fun `le cooldown est actif juste apres avoir affiche une pub`() {
        var now = 1_000L
        val manager = AdFrequencyManager(cooldownMs = 90_000L, clock = { now })

        manager.markAdShown()

        assertTrue(manager.isCooldownActive())
        assertTrue(manager.shouldSkipAd())

        now = 91_001L
        assertFalse(manager.isCooldownActive())
        assertFalse(manager.shouldSkipAd())
    }

    @Test
    fun `un adblock suspect bloque le bypass cooldown`() {
        val now = 1_000L
        val manager = AdFrequencyManager(cooldownMs = 90_000L, clock = { now })

        manager.markAdShown()
        manager.markAdBlockerSuspected()

        assertTrue(manager.isAdBlockerSuspected())
        assertFalse(manager.shouldSkipAd())

        manager.clearAdBlockerSuspected()
        assertTrue(manager.shouldSkipAd())
    }
}


