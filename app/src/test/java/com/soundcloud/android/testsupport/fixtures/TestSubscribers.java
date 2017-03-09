package com.soundcloud.android.testsupport.fixtures;

import static org.mockito.Mockito.mock;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.rx.eventbus.TestEventBus;

import javax.inject.Provider;

public class TestSubscribers {

    public static Provider<ExpandPlayerSubscriber> expandPlayerSubscriber() {
        return () -> new ExpandPlayerSubscriber(new TestEventBus(), mock(PlaybackToastHelper.class), mock(MiniplayerExperiment.class), mock(PerformanceMetricsEngine.class));
    }

}
