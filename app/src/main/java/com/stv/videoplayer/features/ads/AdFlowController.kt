package com.stv.videoplayer.features.ads

import android.app.Activity
import android.content.Context
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
    private val frequencyManager: AdFrequencyManager = AdFrequencyManager.shared(),
    private val isAdBlockerActive: suspend (Context) -> Boolean = { context ->
        AdBlockerDetector.isAdBlockerActive(context)
    }
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
        if (!bypassCooldown && frequencyManager.shouldSkipAd()) {
            Log.d(tag, "Cooldown active -> skipping ad")
            return AdResult.SkippedByCooldown
        }

        for (attempt in 1..autoRetries) {
            Log.d(tag, "Ad attempt $attempt / $autoRetries")
            val result = tryOnce(activity, onAdShowed)

            when (result) {
                AdResult.AdDismissed -> return result
                AdResult.SkippedByNoFill -> return result
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
                    Log.w(tag, "Ad load timeout (${adLoadTimeoutMs}ms); running cross-check")
                    resolveNetworkFailure(activity.applicationContext, cont, null)
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
                        onNoFill = {
                            timeoutJob.cancel()
                            Log.i(tag, "AdMob returned no-fill; starting playback without blocking")
                            if (cont.isActive) cont.resume(AdResult.SkippedByNoFill)
                        },
                        onNoNetwork = {
                            timeoutJob.cancel()
                            Log.d(tag, "No network")
                            if (cont.isActive) cont.resume(AdResult.NoNetwork)
                        },
                        onNetworkError = {
                            timeoutJob.cancel()
                            if (cont.isActive) {
                                resolveNetworkFailure(activity.applicationContext, cont, timeoutJob)
                            }
                        },
                        isRequestActive = { cont.isActive }
                    )
                }
            }
        }
    }

    private fun resolveNetworkFailure(
        context: Context,
        cont: kotlinx.coroutines.CancellableContinuation<AdResult>,
        timeoutJob: kotlinx.coroutines.Job?
    ) {
        CoroutineScope(cont.context).launch {
            val blocked = isAdBlockerActive(context)
            if (!cont.isActive) return@launch
            timeoutJob?.cancel()
            if (blocked) {
                Log.w(tag, "Ad cross-check confirmed an active ad blocker")
                cont.resume(AdResult.AdBlockerBlocked)
            } else {
                Log.i(tag, "Ad cross-check found no blocker; starting playback")
                cont.resume(AdResult.SkippedByTimeout)
            }
        }
    }

    sealed class AdResult {
        data object AdDismissed : AdResult()
        data object SkippedByNoFill : AdResult()
        data object SkippedByCooldown : AdResult()
        data object SkippedByTimeout : AdResult()
        data object AdBlockerBlocked : AdResult()
        data object Failed : AdResult()
        data object NeedRetry : AdResult()
        data object NoNetwork : AdResult()
    }
}


