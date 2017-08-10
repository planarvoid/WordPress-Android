package com.soundcloud.android.playback;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExpandPlayerObserverTest {

    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    private ExpandPlayerObserver observer;
    private final TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        observer = new ExpandPlayerObserver(eventBus, playbackFeedbackHelper, performanceMetricsEngine);
    }

    @Test
    public void showsPlayerOnSuccessfulPlaybackResult() {
        observer.onNext(PlaybackResult.success());

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isAutomaticExpand()).isTrue();
    }

    @Test
    public void showsFeedbackOnPlaybackResultError() {
        PlaybackResult errorResult = PlaybackResult.error(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        observer.onNext(errorResult);

        verify(playbackFeedbackHelper).showFeedbackOnPlaybackError(errorResult.getErrorReason());
    }
}
