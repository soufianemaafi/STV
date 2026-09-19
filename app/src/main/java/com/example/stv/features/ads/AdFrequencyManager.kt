package com.example.stv.features.ads

/**
 * Gère le cooldown anti-zapping des interstitiels.
 *
 * Règle : si une pub vient d'être vue, on peut la bypasser pendant un délai
 * configurable pour garantir un zapping rapide sans spam publicitaire.
 *
 * L'état est partagé au niveau de l'application pour survivre aux rotations d'écran.
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
        const val DEFAULT_COOLDOWN_MS = 180_000L

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

