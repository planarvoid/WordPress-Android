package com.soundcloud.android.playback;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackMeterTest {

    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock PlayStateEvent playStateEvent;

    private EventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        new PlaybackMeter(eventBus, performanceMetricsEngine).subscribe();
    }

    @Test
    public void publishesEndMeasuringsOnPlayTransition() {
        when(playStateEvent.isPlayerPlaying()).thenReturn(true);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, playStateEvent);

        verify(performanceMetricsEngine).endMeasuring(MetricType.TIME_TO_PLAY);
        verify(performanceMetricsEngine).endMeasuring(MetricType.TIME_TO_SKIP);
    }
}
