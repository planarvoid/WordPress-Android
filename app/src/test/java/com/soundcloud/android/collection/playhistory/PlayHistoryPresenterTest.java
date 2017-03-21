package com.soundcloud.android.collection.playhistory;


import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;
import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.Fragment;

import javax.inject.Provider;
import java.util.List;

public class PlayHistoryPresenterTest extends AndroidUnitTest {

    private static final int LISTENING_HISTORY_SIZE = 10;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    @Mock private PlayHistoryOperations playHistoryOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private SwipeRefreshAttacher swipeRrefreshAttacher;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private PlayHistoryAdapter adapter;
    @Mock private FeedbackController feedbackController;
    @Mock private Fragment fragment;
    @Mock private OfflinePropertiesProvider offlinePropertiesProvider;
    @Mock private FeatureFlags featureFlags;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Provider expandPlayerSubscriberProvider = providerOf(expandPlayerSubscriber);

    private PlayHistoryPresenter playHistoryPresenter;

    @Before
    public void setUp() throws Exception {

        List<TrackItem> trackItems = ModelFixtures.trackItems(LISTENING_HISTORY_SIZE);
        when(playHistoryOperations.playHistory()).thenReturn(Observable.just(trackItems));

        playHistoryPresenter = new PlayHistoryPresenter(playHistoryOperations,
                                                        offlineContentOperations,
                                                        adapter,
                                                        expandPlayerSubscriberProvider,
                                                        eventBus,
                                                        swipeRrefreshAttacher,
                                                        feedbackController,
                                                        offlinePropertiesProvider,
                                                        featureFlags,
                                                        performanceMetricsEngine);
    }

    @Test
    public void clearsPlayHistoryWhenConfirmationClicked() throws Exception {
        when(playHistoryOperations.clearHistory()).thenReturn(Observable.just(true));
        playHistoryPresenter.onCreate(fragment, null);

        playHistoryPresenter.onClearConfirmationClicked();

        verify(adapter).clear();
        verify(feedbackController, never()).showFeedback(any(Feedback.class));
    }

    @Test
    public void showsErrorMessageWhenClearPlayHistoryFails() throws Exception {
        when(playHistoryOperations.clearHistory()).thenReturn(Observable.just(false));
        playHistoryPresenter.onCreate(fragment, null);

        playHistoryPresenter.onClearConfirmationClicked();

        verify(adapter, never()).clear();
        verify(feedbackController).showFeedback(Feedback.create(R.string.collections_play_history_clear_error_message,
                                                                LENGTH_LONG));
    }

    @Test
    public void shouldEndListeningHistoryLoadMeasurement() {
        playHistoryPresenter.onCreate(fragment, null);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        assertThat(performanceMetricArgumentCaptor.getValue())
                .hasMetricType(MetricType.LISTENING_HISTORY_LOAD)
                .containsMetricParam(MetricKey.LISTENING_HISTORY_SIZE, LISTENING_HISTORY_SIZE + 1); //+1 for the header
    }

    @Test
    public void shouldSetTrackClickListener() throws Exception {
        verify(adapter).setTrackClickListener(playHistoryPresenter);
    }
}
