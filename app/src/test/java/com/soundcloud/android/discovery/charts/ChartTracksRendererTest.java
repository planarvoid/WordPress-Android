package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.api.model.ChartCategory.MUSIC;
import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.view.View;

import java.util.List;

public class ChartTracksRendererTest extends AndroidUnitTest {
    private static final int POSITION = 1;
    private static final Urn GENRE_URN = new Urn("soundcloud:genres:rock");
    private static final Optional<Urn> QUERY_URN = Optional.of(new Urn("soundcloud:charts:2345kj235kj2435"));
    private static final ChartTrackListItem.Track ITEM_1 = createChartTrackListItem();
    private static final ChartTrackListItem.Track ITEM_2 = createChartTrackListItem();
    private static final List<ChartTrackListItem> ITEMS = Lists.<ChartTrackListItem>newArrayList(ITEM_1, ITEM_2);
    private static final String SCREEN = "screen";
    private static final TrackSourceInfo TRACK_SOURCE_INFO = new TrackSourceInfo(SCREEN, true);

    @Mock private TrackItemRenderer trackItemRenderer;
    @Mock private View itemView;
    @Mock private ScreenProvider screenProvider;
    @Captor private ArgumentCaptor<ChartTrackItem> chartTrackItemCaptor;
    @Captor private ArgumentCaptor<View> viewCaptor;
    @Captor private ArgumentCaptor<Integer> positionCaptor;
    @Captor private ArgumentCaptor<Optional<TrackSourceInfo>> trackSourceInfoCaptor;

    @Test
    public void shouldRenderPositionalTrack() {
        TRACK_SOURCE_INFO.setQuerySourceInfo(QuerySourceInfo.create(0, QUERY_URN.get()));

        when(screenProvider.getLastScreenTag()).thenReturn(SCREEN);

        ChartTracksRenderer chartTracksRenderer = new ChartTracksRenderer(trackItemRenderer, screenProvider);
        chartTracksRenderer.bindItemView(POSITION, itemView, ITEMS);
        verify(trackItemRenderer).bindChartTrackView(ITEM_2.chartTrackItem, itemView, POSITION, Optional.of(TRACK_SOURCE_INFO));
    }

    private static ChartTrackListItem.Track createChartTrackListItem() {
        ChartTrackItem chartTrackItem = new ChartTrackItem(TOP, trackItem(), MUSIC, GENRE_URN, QUERY_URN);
        return ChartTrackListItem.forTrack(chartTrackItem);
    }
}
