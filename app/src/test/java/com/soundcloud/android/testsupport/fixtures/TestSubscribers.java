package com.soundcloud.android.testsupport.fixtures;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.rx.eventbus.TestEventBus;

import javax.inject.Provider;

public class TestSubscribers {

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber() {
        return expandPlayerSubscriber(new TestEventBus());
    }

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber(TestEventBus eventBus) {
        return () -> {
            MiniplayerExperiment mockExperiment = mock(MiniplayerExperiment.class);
            when(mockExperiment.canExpandPlayer()).thenReturn(true);
            return new ExpandPlayerSubscriber(eventBus, mock(PlaybackFeedbackHelper.class), mockExperiment, mock(PerformanceMetricsEngine.class));
        };
    }

}
