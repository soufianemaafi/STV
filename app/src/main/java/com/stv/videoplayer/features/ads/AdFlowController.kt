package com.stv.videoplayer.features.ads

import android.app.Activity
import android.util.Log
import com.stv.videoplayer.AdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Orchestre le flux publicitaire avec cooldown anti-zapping et dÃ©tection
 * du blocage publicitaire via Ã©chec de chargement.
 */
class AdFlowController(
    private val adManager: AdManager,
    private val frequencyManager: AdFrequencyManager = AdFrequencyManager.shared()
) {

    init {
        adManager.preloadInterstitial()
    }

    private val tag = "AdFlowController"
    private val autoRetries = 3
    private val retryDelayMs = 2_000L
    private val adLoadTimeoutMs = 10_000L

    suspend fun showAdIfNeeded(
        activity: Activity,
        bypassCooldown: Boolean = false,
        onAdShowed: () -> Unit = {}
    ): AdResult {
        if (!bypassCooldown && frequencyManager.isAdBlockerSuspected()) {
            Log.d(tag, "Ad blocker already suspected -> blocking playback")
            return AdResult.AdBlockerBlocked
        }

        if (!bypassCooldown && frequencyManager.shouldSkipAd()) {
            Log.d(tag, "Cooldown active -> skipping ad")
            return AdResult.SkippedByCooldown
        }

        for (attempt in 1..autoRetries) {
            Log.d(tag, "Ad attempt $attempt / $autoRetries")
            val result = tryOnce(activity, onAdShowed)

            when (result) {
                AdResult.AdDismissed -> return result
                AdResult.NoNetwork -> return result
                AdResult.SkippedByTimeout -> return result
                AdResult.Failed -> {
                    if (attempt < autoRetries) {
                        delay(retryDelayMs)
                    }
                }
                AdResult.AdBlockerBlocked -> return result
                AdResult.SkippedByCooldown -> return result
                AdResult.NeedRetry -> return result
            }
        }

        Log.w(tag, "Ad unavailable after $autoRetries attempts -> retry required")
        return AdResult.NeedRetry
    }

    private suspend fun tryOnce(
        activity: Activity,
        onAdShowed: () -> Unit
    ): AdResult {
        return suspendCancellableCoroutine { cont ->
            val timeoutJob = CoroutineScope(cont.context).launch {
                delay(adLoadTimeoutMs)
                if (cont.isActive) {
                    Log.w(tag, "Ad load timeout (${adLoadTimeoutMs}ms); keeping playback retryable")
                    cont.resume(AdResult.SkippedByTimeout)
                }
            }

            CoroutineScope(cont.context).launch {
                withContext(Dispatchers.Main.immediate) {
                    adManager.loadAndShow(
                        activity = activity,
                        onAdShowed = {
                            timeoutJob.cancel()
                            frequencyManager.markAdShown()
                            Log.d(tag, "Ad showing -> cooldown timestamp updated")
                            onAdShowed()
                        },
                        onAdDismissed = {
                            timeoutJob.cancel()
                            Log.d(tag, "Ad dismissed")
                            if (cont.isActive) cont.resume(AdResult.AdDismissed)
                        },
                        onFailed = {
                            timeoutJob.cancel()
                            Log.d(tag, "Ad load/show failed; keeping playback retryable")
                            if (cont.isActive) cont.resume(AdResult.Failed)
                        },
                        onNoNetwork = {
                            timeoutJob.cancel()
                            Log.d(tag, "No network")
                            if (cont.isActive) cont.resume(AdResult.NoNetwork)
                        },
                        onNetworkError = {
                            timeoutJob.cancel()
                            if (cont.isActive) {
                                frequencyManager.markAdBlockerSuspected()
                                Log.w(tag, "AdMob network error -> ad blocker suspected")
                                cont.resume(AdResult.AdBlockerBlocked)
                            }
                        },
                        isRequestActive = { cont.isActive }
                    )
                }
            }
        }
    }

    sealed class AdResult {
        data object AdDismissed : AdResult()
        data object SkippedByCooldown : AdResult()
        data object SkippedByTimeout : AdResult()
        data object AdBlockerBlocked : AdResult()
        data object Failed : AdResult()
        data object NeedRetry : AdResult()
        data object NoNetwork : AdResult()
    }
}


