package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class RecentlyPlayedPresenterTest extends AndroidUnitTest {

    private static final int RECENTLY_PLAYED_ITEMS_COUNT = 20;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private FeedbackController feedbackController;
    @Mock private Fragment fragment;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private RecentlyPlayedOperations recentlyPlayedOperations;
    @Mock private RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory;
    @Mock private Resources resources;
    @Mock private RecentlyPlayedAdapter recentlyPlayedAdapter;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();

    private RecentlyPlayedPresenter recentlyPlayedPresenter;

    @Before
    public void setUp() throws Exception {
        when(recentlyPlayedAdapterFactory.create(anyBoolean(), any(SimpleHeaderRenderer.Listener.class)))
                .thenReturn(recentlyPlayedAdapter);

        List<RecentlyPlayedPlayableItem> items = createRecentlyPlayedFixtures(RECENTLY_PLAYED_ITEMS_COUNT);
        when(recentlyPlayedOperations.recentlyPlayed()).thenReturn(Observable.just(items));

        recentlyPlayedPresenter = new RecentlyPlayedPresenter(swipeRefreshAttacher,
                                                              recentlyPlayedAdapterFactory,
                                                              resources,
                                                              recentlyPlayedOperations,
                                                              feedbackController,
                                                              eventBus,
                                                              offlinePropertiesProvider,
                                                              featureFlags,
                                                              performanceMetricsEngine);
    }

    @Test
    public void shouldEndRecentlyPlayedLoadMeasurement() {
        recentlyPlayedPresenter.onCreate(fragment, null);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.RECENTLY_PLAYED_LOAD)
                .containsMetricParam(MetricKey.RECENTLY_PLAYED_SIZE, RECENTLY_PLAYED_ITEMS_COUNT + 1); //+1 is for the header
    }

    private static List<RecentlyPlayedPlayableItem> createRecentlyPlayedFixtures(int count) {
        return Collections.nCopies(count, mock(RecentlyPlayedPlayableItem.class));
    }
}
