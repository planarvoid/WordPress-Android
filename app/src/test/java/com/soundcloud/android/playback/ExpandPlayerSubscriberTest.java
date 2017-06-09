package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

public class ExpandPlayerSubscriberTest extends AndroidUnitTest {
    private ExpandPlayerSubscriber subscriber;

    private TestEventBus eventBus;
    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        subscriber = new ExpandPlayerSubscriber(eventBus, playbackFeedbackHelper, performanceMetricsEngine);
    }

    @Test
    public void expandsPlayerOnPlaybackResultSuccess() {
        subscriber.onNext(PlaybackResult.success());

        Robolectric.flushForegroundThreadScheduler();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).isTrue();
    }

    @Test
    public void showsFeedbackOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        subscriber.onNext(errorResult);

        verify(playbackFeedbackHelper).showFeedbackOnPlaybackError(errorResult.getErrorReason());
    }

    @Test
    public void clearsPerformanceMetricsOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        subscriber.onNext(errorResult);

        verify(performanceMetricsEngine).clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER);
    }

    @Test
    public void doesNotClearPerformanceMetricsWhenPlayerIsExpanded() {
        subscriber.onNext(PlaybackResult.success());

        verify(performanceMetricsEngine, never()).clearMeasurement(MetricType.TIME_TO_EXPAND_PLAYER);
    }
}
