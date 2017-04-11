package com.soundcloud.android.collection;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;

public class CollectionPreviewRendererTest extends AndroidUnitTest {

    @Mock private Navigator navigator;
    @Mock private Resources resources;
    @Mock private FeatureOperations featureOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private View view;
    @Mock private FeatureFlags featureFlags;

    private CollectionPreviewRenderer collectionPreviewRenderer;

    @Before
    public void setUp() throws Exception {
        collectionPreviewRenderer = new CollectionPreviewRenderer(navigator, resources, featureOperations, imageOperations, performanceMetricsEngine, featureFlags);
    }

    @Test
    public void shouldStartLikedStationsLoadingOnGotToLikedStations() {
        collectionPreviewRenderer.onGoToStationsClick(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LIKED_STATIONS_LOAD);
    }
}
