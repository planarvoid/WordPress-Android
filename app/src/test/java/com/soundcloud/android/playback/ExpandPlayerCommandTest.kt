package com.soundcloud.android.playback

import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.analytics.performance.MetricType
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper
import com.soundcloud.android.properties.FeatureFlags
import com.soundcloud.android.properties.Flag
import com.soundcloud.rx.eventbus.TestEventBusV2
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExpandPlayerCommandTest {

    @Mock lateinit var expandPlayerCommand: ExpandPlayerCommand

    private var eventBus: TestEventBusV2 = TestEventBusV2()

    @Mock lateinit var playbackFeedbackHelper: PlaybackFeedbackHelper
    @Mock lateinit var playSessionStateStorage: PlaySessionStateStorage
    @Mock lateinit var performanceMetricsEngine: PerformanceMetricsEngine
    @Mock lateinit var featureFlags: FeatureFlags

    @Before
    fun setUp() {
        expandPlayerCommand = ExpandPlayerCommand(playSessionStateStorage, playbackFeedbackHelper, performanceMetricsEngine, featureFlags, eventBus)
    }

    @Test
    fun `shows feedback on playback result error`() {
        val errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS)

        expandPlayerCommand.call(errorResult)

        verify(playbackFeedbackHelper).showFeedbackOnPlaybackError(errorResult.errorReason)
    }

    @Test
    fun `allows multiple playback results`() {
        expandPlayerCommand.call(PlaybackResult.success())
        expandPlayerCommand.call(PlaybackResult.success())
        expandPlayerCommand.call(PlaybackResult.success())

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size).isEqualTo(3)
    }

    @Test
    fun `clears performance metrics on playback result error`() {
        val errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE)

        expandPlayerCommand.call(errorResult)

        verify(performanceMetricsEngine).clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER)
    }

    @Test
    fun `does not clear performance metrics when player is expanded`() {
        expandPlayerCommand.call(PlaybackResult.success())

        verify(performanceMetricsEngine, never()).clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER)
    }

    @Test
    fun `shows player on successful playback result`() {
        whenever(featureFlags.isEnabled(Flag.MINI_PLAYER)).thenReturn(false)
        expandPlayerCommand.call(PlaybackResult.success())

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isAutomaticExpand).isTrue()
    }

    @Test
    fun `shows player on first playback`() {
        whenever(featureFlags.isEnabled(Flag.MINI_PLAYER)).thenReturn(true)
        whenever(playSessionStateStorage.hasPlayed()).thenReturn(false)

        expandPlayerCommand.call(PlaybackResult.success())

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isAutomaticExpand).isTrue()
    }

    @Test
    fun `does not show player on subsequent plays`() {
        whenever(featureFlags.isEnabled(Flag.MINI_PLAYER)).thenReturn(true)
        whenever(playSessionStateStorage.hasPlayed()).thenReturn(true)

        expandPlayerCommand.call(PlaybackResult.success())

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isShow).isTrue()
    }

}
