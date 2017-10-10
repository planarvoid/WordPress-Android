package com.soundcloud.android.playback.playqueue;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.view.snackbar.FeedbackController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.widget.ToggleButton;

@RunWith(MockitoJUnitRunner.class)
public class PlayQueueViewTest {

    @Mock PlayQueuePresenter playQueuePresenter;
    @Mock PlayQueueAdapter playQueueAdapter;
    @Mock PlayQueueSwipeToRemoveCallbackFactory playQueueSwipeToRemoveCallbackFactory;
    @Mock FeedbackController feedbackController;
    @Mock SmoothScrollLinearLayoutManager smoothScrollLinearLayoutManager;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock ToggleButton toggleButton;
    @Mock TrackPlayQueueItemRenderer trackPlayQueueItemRenderer;
    @Mock HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer;
    @Mock MagicBoxPlayQueueItemRenderer magicBoxPlayQueueItemRenderer;

    private PlayQueueView playQueueView;

    @Before
    public void setUp() throws Exception {
        playQueueView = new PlayQueueView(playQueuePresenter,
                                          playQueueSwipeToRemoveCallbackFactory,
                                          feedbackController,
                                          smoothScrollLinearLayoutManager,
                                          performanceMetricsEngine,
                                          trackPlayQueueItemRenderer,
                                          headerPlayQueueItemRenderer,
                                          magicBoxPlayQueueItemRenderer);
    }

    @Test
    public void shouldStartMeasurePlayQueueShuffleTimeOnShuffleClicked() {

        playQueueView.shuffleClicked(toggleButton);

        verify(performanceMetricsEngine).startMeasuring(MetricType.PLAY_QUEUE_SHUFFLE);
    }
}
