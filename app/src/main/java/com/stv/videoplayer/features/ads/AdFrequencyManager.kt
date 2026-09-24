package com.stv.videoplayer.features.ads

/**
 * GÃ¨re le cooldown anti-zapping des interstitiels.
 *
 * RÃ¨gle : si une pub vient d'Ãªtre vue, on peut la bypasser pendant un dÃ©lai
 * configurable pour garantir un zapping rapide sans spam publicitaire.
 *
 * L'Ã©tat est partagÃ© au niveau de l'application pour survivre aux rotations d'Ã©cran.
 */
class AdFrequencyManager(
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private var lastAdShownAtMs: Long? = null
    private var adBlockerSuspected: Boolean = false

    fun isCooldownActive(nowMs: Long = clock()): Boolean {
        val lastShown = lastAdShownAtMs ?: return false
        return nowMs - lastShown < cooldownMs
    }

    fun shouldSkipAd(nowMs: Long = clock()): Boolean {
        return isCooldownActive(nowMs) && !adBlockerSuspected
    }

    fun isAdBlockerSuspected(): Boolean = adBlockerSuspected

    fun markAdShown(nowMs: Long = clock()) {
        lastAdShownAtMs = nowMs
        adBlockerSuspected = false
    }

    fun markAdBlockerSuspected() {
        adBlockerSuspected = true
    }

    @Suppress("unused")
    fun clearAdBlockerSuspected() {
        adBlockerSuspected = false
    }

    @Suppress("unused")
    fun reset() {
        lastAdShownAtMs = null
        adBlockerSuspected = false
    }

    companion object {
        const val DEFAULT_COOLDOWN_MS = 90_000L

        @Volatile
        private var sharedInstance: AdFrequencyManager? = null

        fun shared(
            cooldownMs: Long = DEFAULT_COOLDOWN_MS,
            clock: () -> Long = System::currentTimeMillis
        ): AdFrequencyManager {
            return sharedInstance ?: synchronized(this) {
                sharedInstance ?: AdFrequencyManager(cooldownMs, clock).also { sharedInstance = it }
            }
        }
    }
}


