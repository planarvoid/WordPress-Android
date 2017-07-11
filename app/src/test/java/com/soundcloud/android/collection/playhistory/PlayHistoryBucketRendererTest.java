package com.soundcloud.android.collection.playhistory;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.navigation.NavigationExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class PlayHistoryBucketRendererTest {

    @Mock NavigationExecutor navigationExecutor;
    @Mock PlayHistoryAdapter adapter;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock View view;

    private PlayHistoryBucketRenderer playHistoryBucketRenderer;

    @Before
    public void setUp() throws Exception {
        playHistoryBucketRenderer = new PlayHistoryBucketRenderer(adapter, navigationExecutor, performanceMetricsEngine);
    }

    @Test
    public void onViewClickedShouldStartListeningHistoryLoadMeasurement() {
        playHistoryBucketRenderer.onViewAllClicked(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LISTENING_HISTORY_LOAD);
    }
}
