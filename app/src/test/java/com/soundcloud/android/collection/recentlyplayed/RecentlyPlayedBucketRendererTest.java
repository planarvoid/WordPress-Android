package com.soundcloud.android.collection.recentlyplayed;

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
public class RecentlyPlayedBucketRendererTest {

    @Mock NavigationExecutor navigationExecutor;
    @Mock RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory;
    @Mock View view;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;

    private RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer;

    @Before
    public void setUp() throws Exception {
        recentlyPlayedBucketRenderer = new RecentlyPlayedBucketRenderer(recentlyPlayedAdapterFactory, navigationExecutor, performanceMetricsEngine);
    }

    @Test
    public void onViewClickedShouldStartRecentlyPlayedLoadMeasurement() {
        recentlyPlayedBucketRenderer.onViewAllClicked(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.RECENTLY_PLAYED_LOAD);
    }
}
