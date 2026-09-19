package com.example.stv.features.ads

import android.app.Activity
import android.util.Log
import com.example.stv.AdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Orchestre le flux publicitaire avec cooldown anti-zapping et détection
 * du blocage publicitaire via échec de chargement.
 */
class AdFlowController(
    private val adManager: AdManager,
    private val frequencyManager: AdFrequencyManager = AdFrequencyManager.shared()
) {

    private val tag = "AdFlowController"
    private val autoRetries = 3
    private val retryDelayMs = 2_000L

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
                AdResult.Failed -> {
                    if (attempt < autoRetries) {
                        delay(retryDelayMs)
                    }
                }
                AdResult.AdBlockerBlocked -> return result
                AdResult.SkippedByCooldown -> return result
            }
        }

        frequencyManager.markAdBlockerSuspected()
        return AdResult.AdBlockerBlocked
    }

    private suspend fun tryOnce(
        activity: Activity,
        onAdShowed: () -> Unit
    ): AdResult {
        return suspendCancellableCoroutine { cont ->
            val timeoutJob = CoroutineScope(cont.context).launch {
                delay(10_000)
                if (cont.isActive) {
                    Log.w(tag, "Ad load timeout (10s)")
                    frequencyManager.markAdBlockerSuspected()
                    cont.resume(AdResult.Failed)
                }
            }

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
                    frequencyManager.markAdBlockerSuspected()
                    Log.d(tag, "Ad failed")
                    if (cont.isActive) cont.resume(AdResult.Failed)
                },
                onNoNetwork = {
                    timeoutJob.cancel()
                    Log.d(tag, "No network")
                    if (cont.isActive) cont.resume(AdResult.NoNetwork)
                }
            )
        }
    }

    sealed class AdResult {
        data object AdDismissed : AdResult()
        data object SkippedByCooldown : AdResult()
        data object AdBlockerBlocked : AdResult()
        data object Failed : AdResult()
        data object NoNetwork : AdResult()
    }
}

