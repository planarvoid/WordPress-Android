package com.soundcloud.android.playback

import com.soundcloud.android.analytics.performance.MetricType
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlayerUICommand
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper
import com.soundcloud.android.properties.FeatureFlags
import com.soundcloud.android.properties.Flag
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.rx.eventbus.EventBusV2
import javax.inject.Inject

@OpenForTesting
class ExpandPlayerCommand
@Inject
constructor(private val playSessionStateStorage: PlaySessionStateStorage,
            private val playbackFeedbackHelper: PlaybackFeedbackHelper,
            private val performanceMetricsEngine: PerformanceMetricsEngine,
            private val featureFlags: FeatureFlags,
            private val eventBus: EventBusV2) {

    fun call(result: PlaybackResult) {
        if (result.isSuccess) {
            when {
                featureFlagDisabled() -> expandPlayer()
                firstTimePlay() -> expandPlayer()
                subsequentPlay() -> showPlayer()
            }
        } else {
            performanceMetricsEngine.clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER)
            playbackFeedbackHelper.showFeedbackOnPlaybackError(result.errorReason)
        }

    }

    private fun featureFlagDisabled() = !featureFlags.isEnabled(Flag.MINI_PLAYER)

    private fun firstTimePlay() = featureFlags.isEnabled(Flag.MINI_PLAYER) && !playSessionStateStorage.hasPlayed()

    private fun subsequentPlay() = featureFlags.isEnabled(Flag.MINI_PLAYER) && playSessionStateStorage.hasPlayed()

    private fun showPlayer() {
        performanceMetricsEngine.clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER)
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer())
    }

    private fun expandPlayer() {
        playSessionStateStorage.setPlayedState()
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer())
    }

}
