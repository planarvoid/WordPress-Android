package com.soundcloud.android.discovery;

import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ChartTracksPresenterTest extends AndroidUnitTest {
    final ChartTrackListItem.Header HEADER = ChartTrackListItem.forHeader(TOP);
    final ChartTrackListItem.Track FIRST_TRACK_ITEM = createChartTrackListItem(1);
    final ChartTrackListItem.Track SECOND_TRACK_ITEM = createChartTrackListItem(2);
    final ChartTrackListItem.Track THIRD_TRACK_ITEM = createChartTrackListItem(3);
    final ChartTrackListItem.Footer FOOTER = ChartTrackListItem.forFooter(1);
    final List<ChartTrackListItem> CHART_TRACK_ITEMS = Lists.newArrayList(HEADER,
                                                                          FIRST_TRACK_ITEM,
                                                                          SECOND_TRACK_ITEM,
                                                                          THIRD_TRACK_ITEM,
                                                                          FOOTER);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ChartsOperations chartsOperations;
    @Mock private ChartTracksAdapter chartTracksAdapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    private ChartTracksPresenter chartTracksPresenter;
    private ChartType chartType = TOP;
    private String genre = "all-music";

    @Before
    public void setup() {
        chartTracksPresenter = new ChartTracksPresenter(swipeRefreshAttacher,
                                                        chartsOperations,
                                                        chartTracksAdapter,
                                                        playbackInitiator,
                                                        providerOf(expandPlayerSubscriber));
        when(chartsOperations.firstPagedTracks(TOP, genre)).thenReturn(Observable.<PagedChartTracks>empty());
        when(chartsOperations.nextPagedTracks()).thenReturn(mock(Pager.PagingFunction.class));
        chartTracksPresenter.onBuildBinding(getChartArguments());
    }

    private Bundle getChartArguments() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ChartTracksFragment.EXTRA_TYPE, chartType);
        final Urn urn = new Urn("soundcloud:genre:" + genre);
        bundle.putParcelable(ChartTracksFragment.EXTRA_GENRE_URN, urn);
        return bundle;
    }

    @Test
    public void startsPlaybackFromTrackItem() {
        final ArrayList<Urn> expectedPlayQueue = Lists.newArrayList(FIRST_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    SECOND_TRACK_ITEM.chartTrackItem.getUrn(),
                                                                    THIRD_TRACK_ITEM.chartTrackItem.getUrn());
        final int chartItemPosition = CHART_TRACK_ITEMS.indexOf(SECOND_TRACK_ITEM);
        when(chartTracksAdapter.getItem(chartItemPosition)).thenReturn(SECOND_TRACK_ITEM);
        when(chartTracksAdapter.getItems()).thenReturn(CHART_TRACK_ITEMS);
        when(playbackInitiator.playTracks(anyList(), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.empty());
        chartTracksPresenter.onItemClicked(mock(View.class), chartItemPosition);

        final int expectedPlayQueuePosition = expectedPlayQueue.indexOf(SECOND_TRACK_ITEM.chartTrackItem.getUrn());
        verify(playbackInitiator).playTracks(expectedPlayQueue, expectedPlayQueuePosition, PlaySessionSource.EMPTY);
    }

    @Test
    public void doesNothingWhenHeaderOrFooterClicked() {
        final int headerPosition = CHART_TRACK_ITEMS.indexOf(HEADER);
        when(chartTracksAdapter.getItem(headerPosition)).thenReturn(HEADER);
        chartTracksPresenter.onItemClicked(mock(View.class), headerPosition);
        verifyZeroInteractions(playbackInitiator);

        final int footerPosition = CHART_TRACK_ITEMS.indexOf(FOOTER);
        when(chartTracksAdapter.getItem(footerPosition)).thenReturn(FOOTER);
        chartTracksPresenter.onItemClicked(mock(View.class), CHART_TRACK_ITEMS.indexOf(FOOTER));
        verifyZeroInteractions(playbackInitiator);
    }

    private ChartTrackListItem.Track createChartTrackListItem(int position) {
        return ChartTrackListItem.forTrack(new ChartTrackItem(
                ChartType.TOP,
                PropertySet.create().put(PlayableProperty.URN, Urn.forTrack(position)),
                position
        ));
    }
}
