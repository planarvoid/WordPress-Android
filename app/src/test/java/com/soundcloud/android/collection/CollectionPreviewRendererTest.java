package com.soundcloud.android.collection;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;

public class CollectionPreviewRendererTest extends AndroidUnitTest {

    @Mock private NavigationExecutor navigationExecutor;
    @Mock private Navigator navigator;
    @Mock private Resources resources;
    @Mock private FeatureOperations featureOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private View view;
    @Mock private Activity activity;
    @Mock private FeatureFlags featureFlags;
    @Mock private ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Mock private ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;
    private CollectionPreviewRenderer collectionPreviewRenderer;

    @Before
    public void setUp() throws Exception {
        collectionPreviewRenderer = new CollectionPreviewRenderer(navigationExecutor,
                                                                  navigator,
                                                                  resources,
                                                                  imageOperations,
                                                                  performanceMetricsEngine,
                                                                  changeLikeToSaveExperiment,
                                                                  changeLikeToSaveExperimentStringHelper);
    }

    @Test
    public void shouldStartMeasuringikedStationsLoadingOnGotToLikedStations() {
        collectionPreviewRenderer.onGoToStationsClick(activity);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LIKED_STATIONS_LOAD);
    }

    @Test
    public void shouldStartMeasuringPlaylistsLoadingOnGoToPlaylists() {
        collectionPreviewRenderer.onGoToPlaylistsAndAlbumsClick(activity);

        verify(performanceMetricsEngine).startMeasuring(MetricType.PLAYLISTS_LOAD);
    }

    @Test
    public void shouldStartMeasuringLikedTracksLoadingOnGotToTrackLikes() {
        collectionPreviewRenderer.onGoToTrackLikesClick(view);

        verify(performanceMetricsEngine).startMeasuring(MetricType.LIKED_TRACKS_FIRST_PAGE_LOAD);
    }
}
