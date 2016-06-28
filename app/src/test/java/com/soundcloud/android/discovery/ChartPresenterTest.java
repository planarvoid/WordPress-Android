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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.Pager;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.ArrayList;

public class ChartPresenterTest extends AndroidUnitTest {
    final ChartTrackItem.Header HEADER = ChartTrackItem.forHeader(TOP);
    final ChartTrackItem.Track FIRST_TRACK_ITEM = ChartTrackItem.forTrack(ModelFixtures.create(TrackItem.class));
    final ChartTrackItem.Track SECOND_TRACK_ITEM = ChartTrackItem.forTrack(ModelFixtures.create(TrackItem.class));
    final ChartTrackItem.Track THIRD_TRACK_ITEM = ChartTrackItem.forTrack(ModelFixtures.create(TrackItem.class));
    final ChartTrackItem.Footer FOOTER = ChartTrackItem.forFooter(1);
    final ArrayList<ChartTrackItem> CHART_TRACK_ITEMS = Lists.newArrayList(HEADER,
                                                                           FIRST_TRACK_ITEM,
                                                                           SECOND_TRACK_ITEM,
                                                                           THIRD_TRACK_ITEM,
                                                                           FOOTER);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ChartsOperations chartsOperations;
    @Mock private ChartTrackAdapter chartTrackAdapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    private ChartPresenter chartPresenter;
    private ChartType chartType = TOP;
    private String genre = "all-music";

    @Before
    public void setup() {
        chartPresenter = new ChartPresenter(swipeRefreshAttacher,
                                            chartsOperations,
                                            chartTrackAdapter,
                                            playbackInitiator,
                                            providerOf(expandPlayerSubscriber));
        when(chartsOperations.firstPagedTracks(TOP, genre)).thenReturn(Observable.<PagedChartTracks>empty());
        when(chartsOperations.nextPagedTracks()).thenReturn(mock(Pager.PagingFunction.class));
        chartPresenter.onBuildBinding(getChartArguments());
    }

    @NonNull
    private Bundle getChartArguments() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ChartFragment.EXTRA_TYPE, chartType);
        final Urn urn = new Urn("soundcloud:genre:" + genre);
        bundle.putParcelable(ChartFragment.EXTRA_GENRE_URN, urn);
        return bundle;
    }

    @Test
    public void startsPlaybackFromTrackItem() {
        final ArrayList<Urn> expectedPlayQueue = Lists.newArrayList(FIRST_TRACK_ITEM.trackItem.getUrn(),
                                                                    SECOND_TRACK_ITEM.trackItem.getUrn(),
                                                                    THIRD_TRACK_ITEM.trackItem.getUrn());
        final int chartItemPosition = CHART_TRACK_ITEMS.indexOf(SECOND_TRACK_ITEM);
        when(chartTrackAdapter.getItem(chartItemPosition)).thenReturn(SECOND_TRACK_ITEM);
        when(chartTrackAdapter.getItems()).thenReturn(CHART_TRACK_ITEMS);
        when(playbackInitiator.playTracks(anyList(), anyInt(), any(PlaySessionSource.class))).thenReturn(Observable.empty());
        chartPresenter.onItemClicked(mock(View.class), chartItemPosition);

        final int expectedPlayQueuePosition = expectedPlayQueue.indexOf(SECOND_TRACK_ITEM.trackItem.getUrn());
        verify(playbackInitiator).playTracks(expectedPlayQueue, expectedPlayQueuePosition, PlaySessionSource.EMPTY);
    }

    @Test
    public void doesNothingWhenHeaderOrFooterClicked() {
        final int headerPosition = CHART_TRACK_ITEMS.indexOf(HEADER);
        when(chartTrackAdapter.getItem(headerPosition)).thenReturn(HEADER);
        chartPresenter.onItemClicked(mock(View.class), headerPosition);
        verifyZeroInteractions(playbackInitiator);

        final int footerPosition = CHART_TRACK_ITEMS.indexOf(FOOTER);
        when(chartTrackAdapter.getItem(footerPosition)).thenReturn(FOOTER);
        chartPresenter.onItemClicked(mock(View.class), CHART_TRACK_ITEMS.indexOf(FOOTER));
        verifyZeroInteractions(playbackInitiator);
    }

}
