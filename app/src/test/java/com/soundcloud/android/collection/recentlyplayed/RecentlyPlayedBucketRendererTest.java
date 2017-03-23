package com.soundcloud.android.collection.recentlyplayed;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class RecentlyPlayedBucketRendererTest extends AndroidUnitTest {

    @Mock Navigator navigator;
    @Mock RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory;
    @Mock View view;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;

    private RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer;

    @Before
    public void setUp() throws Exception {
        recentlyPlayedBucketRenderer = new RecentlyPlayedBucketRenderer(recentlyPlayedAdapterFactory, navigator, performanceMetricsEngine);
    }

    @Test
    public void onViewClickedShouldStartRecentlyPlayedLoadMeasurement() {
        recentlyPlayedBucketRenderer.onViewAllClicked(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.RECENTLY_PLAYED_LOAD);
    }
}
