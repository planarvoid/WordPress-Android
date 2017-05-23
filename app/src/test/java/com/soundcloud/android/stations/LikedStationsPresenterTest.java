package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;

public class LikedStationsPresenterTest extends AndroidUnitTest {

    @Rule public FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private StationsOperations stationsOperations;
    @Mock private StationsAdapter stationsAdapter;
    @Mock private Resources resources;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private EventBus eventBus;
    @Mock private StationsNowPlayingController stationsNowPlayingController;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private LikedStationsPresenter likedStationsPresenter;

    @Before
    public void setUp() throws Exception {

        Urn urn = Urn.forTrackStation(0);
        when(stationsOperations.collection(StationsCollectionsTypes.LIKED))
                .thenReturn(Observable.just(StationFixtures.getStation(urn)));
        when(playQueueManager.getCollectionUrn()).thenReturn(urn);

        likedStationsPresenter = new LikedStationsPresenter(swipeRefreshAttacher,
                                                            stationsOperations,
                                                            stationsAdapter,
                                                            resources,
                                                            playQueueManager,
                                                            eventBus,
                                                            stationsNowPlayingController,
                                                            performanceMetricsEngine,
                                                            changeLikeToSaveExperimentStringHelper);
    }

    @Test
    public void shoudEndMeasuringStationsLoadingPerformance() {
        likedStationsPresenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.LIKED_STATIONS_LOAD)
                .containsMetricParam(MetricKey.STATIONS_COUNT, 1);
    }
}
