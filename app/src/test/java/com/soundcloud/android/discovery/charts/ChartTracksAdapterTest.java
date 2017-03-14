package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.api.model.ChartCategory.MUSIC;
import static com.soundcloud.android.api.model.ChartType.TOP;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

public class ChartTracksAdapterTest extends AndroidUnitTest{

    private static final ChartTrackListItem.Header HEADER = ChartTrackListItem.Header.create(TOP);
    private static final ChartTrackListItem.Track FIRST_TRACK_ITEM = createChartTrackListItem();
    private static final ChartTrackListItem.Track SECOND_TRACK_ITEM = createChartTrackListItem();
    private static final ChartTrackListItem.Track THIRD_TRACK_ITEM = createChartTrackListItem();
    private static final ChartTrackListItem.Footer FOOTER = ChartTrackListItem.Footer.create(new Date(10));

    @Mock private ChartTracksAdapter chartsTracksAdapter;
    @Mock private ChartTracksRenderer chartTracksRenderer;
    @Mock private ChartTracksHeaderRenderer chartTracksHeaderRenderer;
    @Mock private ChartTracksFooterRenderer chartTracksFooterRenderer;

    @Before
    public void setUp() {
        this.chartsTracksAdapter = new ChartTracksAdapter(chartTracksRenderer,
                                                          chartTracksHeaderRenderer,
                                                          chartTracksFooterRenderer);
        chartsTracksAdapter.addItem(HEADER);
        chartsTracksAdapter.addItem(FIRST_TRACK_ITEM);
        chartsTracksAdapter.addItem(SECOND_TRACK_ITEM);
        chartsTracksAdapter.addItem(THIRD_TRACK_ITEM);
        chartsTracksAdapter.addItem(FOOTER);
    }

    private static ChartTrackListItem.Track createChartTrackListItem() {
        TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        return ChartTrackListItem.Track.create(new ChartTrackItem(TOP, trackItem, MUSIC, new Urn("soundcloud:genres:rock"), Optional.absent()));
    }

    @Test
    public void updatesPlayingStateForCurrentPlayingTrack() {
        chartsTracksAdapter.updateNowPlaying(SECOND_TRACK_ITEM.chartTrackItem().getUrn());
        assertThat(((ChartTrackListItem.Track) chartsTracksAdapter.getItem(1)).chartTrackItem().getTrackItem().isPlaying()).isFalse();
        assertThat(((ChartTrackListItem.Track) chartsTracksAdapter.getItem(2)).chartTrackItem().getTrackItem().isPlaying()).isTrue();
        assertThat(((ChartTrackListItem.Track) chartsTracksAdapter.getItem(3)).chartTrackItem().getTrackItem().isPlaying()).isFalse();
    }

}