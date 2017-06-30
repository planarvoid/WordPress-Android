package com.soundcloud.android.testsupport.fixtures;

import static org.mockito.Mockito.mock;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.rx.eventbus.TestEventBusV2;

import javax.inject.Provider;

public class TestSubscribers {

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber() {
        return expandPlayerSubscriber(new TestEventBus());
    }

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber(TestEventBus eventBus) {
        return () -> new ExpandPlayerSubscriber(eventBus, mock(PlaybackFeedbackHelper.class), mock(PerformanceMetricsEngine.class));
    }

    public static Provider<ExpandPlayerSingleObserver> expandPlayerObserver(TestEventBusV2 eventBus) {
        return () -> new ExpandPlayerSingleObserver(eventBus, mock(PlaybackFeedbackHelper.class), mock(PerformanceMetricsEngine.class));
    }

}
