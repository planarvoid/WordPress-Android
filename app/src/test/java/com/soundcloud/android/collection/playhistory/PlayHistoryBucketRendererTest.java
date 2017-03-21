package com.soundcloud.android.collection.playhistory;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class PlayHistoryBucketRendererTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock PlayHistoryAdapter adapter;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;
    @Mock View view;

    private PlayHistoryBucketRenderer playHistoryBucketRenderer;

    @Before
    public void setUp() throws Exception {
        playHistoryBucketRenderer = new PlayHistoryBucketRenderer(adapter, navigator, performanceMetricsEngine);
    }

    @Test
    public void onViewClickedShouldStartListeningHistoryLoadMeasurement() {
        playHistoryBucketRenderer.onViewAllClicked(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LISTENING_HISTORY_LOAD);
    }
}