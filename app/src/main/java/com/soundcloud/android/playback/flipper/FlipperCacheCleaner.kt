package com.soundcloud.android.playback.flipper

import android.content.SharedPreferences
import android.util.Log
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.MetricEvent
import com.soundcloud.android.events.ReferringEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.android.properties.FeatureFlags
import com.soundcloud.android.properties.Flag
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.extensions.commit
import com.soundcloud.annotations.VisibleForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.reporting.DataPoint
import com.soundcloud.reporting.Metric
import com.soundcloud.rx.eventbus.EventBusV2
import javax.inject.Inject

@OpenForTesting
class FlipperCacheCleaner
@Inject
constructor(val featureFlags: FeatureFlags,
            val sharedPreferences: SharedPreferences,
            val flipperConfiguration: FlipperConfiguration,
            val eventBus: EventBusV2) {

    fun clearCacheIfNecessary() {
        if (featureFlags.isEnabled(Flag.CLEAR_FLIPPER_CACHE) && !hasAlreadyClearedCache()) {
            if (setAlreadyClearedCache()) {
                if (isEmptyCache()) {
                    logResult(Result.WAS_ALREADY_EMPTY)
                } else {
                    if (clearCache()) {
                        logResult(Result.SUCCESS)
                    } else {
                        logResult(Result.FAILURE)
                    }
                }
            } else {
                logResult(Result.FAILED_TO_SET_SHARED_PREFERENCES)
            }
        }
    }

    private fun hasAlreadyClearedCache() = sharedPreferences.getBoolean(HAS_ALREADY_CLEARED_FLIPPER_CACHE, false)

    private fun setAlreadyClearedCache() = sharedPreferences.commit(HAS_ALREADY_CLEARED_FLIPPER_CACHE, true)

    private fun clearCache() = flipperConfiguration.cache.clearCache()

    private fun isEmptyCache() = flipperConfiguration.cache.isEmpty()

    private fun logResult(result: Result) {
        sendFabricLog(result)
        sendClearFlipperCacheEvent(result)
    }

    private fun sendClearFlipperCacheEvent(result: Result) {
        eventBus.publish(EventQueue.TRACKING, ClearFlipperCacheEvent(result = result))
    }

    private fun sendFabricLog(result: Result) {
        ErrorUtils.log(Log.INFO, TAG, "Attempt to clear Flipper cache: ${result.key}")
    }

    data class ClearFlipperCacheEvent(val id: String = defaultId(),
                                      val ts: Long = TrackingEvent.defaultTimestamp(),
                                      val referringEvent: Optional<ReferringEvent> = Optional.absent(),
                                      val result: Result) : TrackingEvent(), MetricEvent {
        override fun id() = id

        override fun timestamp() = ts

        override fun referringEvent() = referringEvent

        override fun toMetric(): Metric = Metric.create("ClearFlipperCache", DataPoint.string("result", result.key))

        override fun putReferringEvent(referringEvent: ReferringEvent): TrackingEvent = copy(referringEvent = Optional.of(referringEvent))
    }

    enum class Result(val key: String) {
        SUCCESS("success"),
        FAILURE("failure"),
        WAS_ALREADY_EMPTY("was_already_empty"),
        FAILED_TO_SET_SHARED_PREFERENCES("failed_to_set_shared_preferences")
    }

    companion object {
        @VisibleForTesting
        val HAS_ALREADY_CLEARED_FLIPPER_CACHE = "has_already_cleared_flipper_cache"

        val TAG = "FlipperCacheCleaner"
    }
}
