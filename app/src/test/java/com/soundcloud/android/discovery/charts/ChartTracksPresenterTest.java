package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.api.model.ChartCategory.MUSIC;
import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChartTracksPresenterTest extends AndroidUnitTest {
    private static final ChartType CHART_TYPE = TOP;
    private static final String GENRE = "rock";
    private static final Urn GENRE_URN = new Urn("soundcloud:genres:" + GENRE);
    private static final Urn QUERY_URN = new Urn("soundcloud:charts:1235kj234n5j234523k45j");
    private static final ChartTrackListItem.Header HEADER = ChartTrackListItem.forHeader(TOP);
    private static final ChartTrackListItem.Track FIRST_TRACK_ITEM = createChartTrackListItem(1, Optional.of(QUERY_URN));
    private static final ChartTrackListItem.Track SECOND_TRACK_ITEM = createChartTrackListItem(2, Optional.of(QUERY_URN));
    private static final ChartTrackListItem.Track THIRD_TRACK_ITEM = createChartTrackListItem(3, Optional.<Urn>absent());
    private static final ChartTrackListItem.Footer FOOTER = ChartTrackListItem.forFooter(new Date(10));
    private static final ApiChart<ApiTrack> API_CHART = ChartsFixtures.createApiChart(GENRE, CHART_TYPE);
    private static final ApiChart<ApiTrack> API_CHART_NO_QUERY_URN = ChartsFixtures.createApiChart(GENRE, CHART_TYPE, null);
    private static final List<ChartTrackListItem> CHART_TRACK_ITEMS = Lists.newArrayList(HEADER,
                                                                          FIRST_TRACK_ITEM,
                                                                          SECOND_TRACK_ITEM,
                                                                          THIRD_TRACK_ITEM,
                                                                          FOOTER);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ChartsOperations chartsOperations;
    @Mock private ChartTracksAdapter chartTracksAdapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ChartsTracker chartsTracker;
    @Mock private Fragment fragment;
    @Mock private EventBus eventBus;
    @Mock private MiniplayerExperiment miniplayerExperiment;
    @Mock private PlaybackToastHelper playbackToastHelper;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Spy private ExpandPlayerSubscriber expandPlayerSubscriber =
            new ExpandPlayerSubscriber(eventBus, playbackToastHelper, miniplayerExperiment, performanceMetricsEngine);

    private ChartTracksPresenter chartTracksPresenter;
    private Bundle bundle;

    @Before
    public void setup() {
        chartTracksPresenter = new ChartTracksPresenter(swipeRefreshAttacher,
                                                        chartsOperations,
                                                        chartTracksAdapter,
                                                        playbackInitiator,
                                                        providerOf(expandPlayerSubscriber),
                                                        chartsTracker);
        when(chartsOperations.tracks(TOP, GENRE)).thenReturn(Observable.just(API_CHART));
        bundle = getChartArguments();
        when(fragment.getArguments()).thenReturn(bundle);
        when(miniplayerExperiment.canExpandPlayer()).thenReturn(true);
    }

    @Test
    public void startsPlaybackWithTrackListPlaySessionSourceFromTrackItemWhenQueryUrnIsPresent() {
        chartTracksPresenter.onCreate(fragment, bundle);
        final ArrayList<Urn> expectedPlayQueue = Lists.newArrayList(FIRST_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    SECOND_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    THIRD_TRACK_ITEM.chartTrackItem.getUrn());
        final int chartItemPosition = CHART_TRACK_ITEMS.indexOf(SECOND_TRACK_ITEM);
        int queryPosition = chartItemPosition - 1;
        when(chartTracksAdapter.getItem(chartItemPosition)).thenReturn(SECOND_TRACK_ITEM);
        when(chartTracksAdapter.getItems()).thenReturn(CHART_TRACK_ITEMS);
        when(playbackInitiator.playTracks(anyList(), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.empty());
        final String screenString = "charts:music_top_50:" + GENRE;
        when(chartsTracker.getScreen(TOP, MUSIC, GENRE_URN)).thenReturn(screenString);

        chartTracksPresenter.onItemClicked(mock(View.class), chartItemPosition);

        final ChartTrackItem chartTrackItem = SECOND_TRACK_ITEM.chartTrackItem;
        final int expectedPlayQueuePosition = expectedPlayQueue.indexOf(chartTrackItem.getUrn());
        final PlaySessionSource playSessionSource = PlaySessionSource.forChart(screenString,
                                                                               queryPosition,
                                                                               QUERY_URN,
                                                                               chartTrackItem.chartType(),
                                                                               chartTrackItem.genre());
        verify(playbackInitiator).playTracks(expectedPlayQueue, expectedPlayQueuePosition, playSessionSource);
    }

    @Test
    public void startsPlaybackWithEmptyPlaySessionFromTrackItemWhenQueryUrnIsNotPresent() {
        chartTracksPresenter.onCreate(fragment, bundle);
        final ArrayList<Urn> expectedPlayQueue = Lists.newArrayList(FIRST_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    SECOND_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    THIRD_TRACK_ITEM.chartTrackItem.getUrn());
        final int chartItemPosition = CHART_TRACK_ITEMS.indexOf(THIRD_TRACK_ITEM);
        when(chartTracksAdapter.getItem(chartItemPosition)).thenReturn(THIRD_TRACK_ITEM);
        when(chartTracksAdapter.getItems()).thenReturn(CHART_TRACK_ITEMS);
        when(playbackInitiator.playTracks(anyList(), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.empty());
        final String screenString = "charts:music_top_50:" + GENRE;
        when(chartsTracker.getScreen(TOP, MUSIC, GENRE_URN)).thenReturn(screenString);

        chartTracksPresenter.onItemClicked(mock(View.class), chartItemPosition);

        final int expectedPlayQueuePosition = expectedPlayQueue.indexOf(THIRD_TRACK_ITEM.chartTrackItem.getUrn());
        final PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
        verify(playbackInitiator).playTracks(expectedPlayQueue, expectedPlayQueuePosition, playSessionSource);
    }

    @Test
    public void doesNothingWhenHeaderOrFooterClicked() {
        chartTracksPresenter.onCreate(fragment, bundle);
        final int headerPosition = CHART_TRACK_ITEMS.indexOf(HEADER);
        when(chartTracksAdapter.getItem(headerPosition)).thenReturn(HEADER);
        chartTracksPresenter.onItemClicked(mock(View.class), headerPosition);
        verifyZeroInteractions(playbackInitiator);

        final int footerPosition = CHART_TRACK_ITEMS.indexOf(FOOTER);
        when(chartTracksAdapter.getItem(footerPosition)).thenReturn(FOOTER);
        chartTracksPresenter.onItemClicked(mock(View.class), CHART_TRACK_ITEMS.indexOf(FOOTER));
        verifyZeroInteractions(playbackInitiator);
    }

    @Test
    public void loadsDataInTrackerWhenQueryUrnAvailable() {
        chartTracksPresenter.onCreate(fragment, bundle);
        verify(chartsTracker).chartDataLoaded(API_CHART.tracks().getQueryUrn().get(),
                                              API_CHART.type(),
                                              API_CHART.category(),
                                              API_CHART.genre());
    }

    @Test
    public void doesNotLoadDataInTrackerWhenQueryUrnNotAvailable() {
        when(chartsOperations.tracks(TOP, GENRE)).thenReturn(Observable.just(API_CHART_NO_QUERY_URN));
        chartTracksPresenter.onCreate(fragment, bundle);
        verifyZeroInteractions(chartsTracker);
    }

    private static ChartTrackListItem.Track createChartTrackListItem(int position, Optional<Urn> queryUrn) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUrn(Urn.forTrack(position));
        return ChartTrackListItem.forTrack(new ChartTrackItem(TOP, apiTrack, MUSIC, GENRE_URN, queryUrn));
    }


    private Bundle getChartArguments() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ChartTracksFragment.EXTRA_TYPE, CHART_TYPE);
        final Urn urn = new Urn("soundcloud:genre:" + GENRE);
        bundle.putParcelable(ChartTracksFragment.EXTRA_GENRE_URN, urn);
        return bundle;
    }
}
