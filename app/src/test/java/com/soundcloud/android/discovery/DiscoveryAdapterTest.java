package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryAdapter.DiscoveryItemListenerBucket;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksBucketItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;
    @Mock private RecommendedStationsBucketRenderer recommendedStationsBucketRenderer;
    @Mock private ChartsBucketItemRenderer chartsBucketItemRenderer;
    @Mock private RecommendationsFooterRenderer recommendationsFooterRenderer;
    @Mock private EmptyDiscoveryItemRenderer emptyDiscoveryItemRenderer;
    @Mock private RecommendedStationsBucketItem stationsBucketItem;
    @Mock private RecommendedTracksBucketItem tracksBucketItem;
    @Mock private PlaylistTagsItem playlistTagItem;
    @Mock private ChartsBucketItem chartsBucketItem;

    private DiscoveryItem searchItem = DiscoveryItem.forSearchItem();
    private DiscoveryAdapter adapter;


    @Before
    public void setUp() throws Exception {
        when(stationsBucketItem.getKind()).thenReturn(RecommendedStationsItem);
        when(tracksBucketItem.getKind()).thenReturn(RecommendedTracksItem);
        when(playlistTagItem.getKind()).thenReturn(PlaylistTagsItem);
        when(chartsBucketItem.getKind()).thenReturn(ChartItem);


        adapter = new DiscoveryAdapter(recommendationBucketRenderer,
                                       playlistTagRenderer,
                                       searchItemRenderer,
                                       recommendedStationsBucketRenderer,
                                       chartsBucketItemRenderer,
                                       recommendationsFooterRenderer,
                                       emptyDiscoveryItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem,
                              playlistTagItem,
                              tracksBucketItem,
                              stationsBucketItem,
                              chartsBucketItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(SearchItem.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(PlaylistTagsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(RecommendedTracksItem.ordinal());
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(RecommendedStationsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(4)).isEqualTo(ChartItem.ordinal());
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListenerBucket itemListener = mock(DiscoveryItemListenerBucket.class);
        adapter.setDiscoveryListener(itemListener);

        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
        verify(searchItemRenderer).setSearchListener(itemListener);
        verify(recommendedStationsBucketRenderer).setListener(itemListener);
    }

    @Test
    public void updatesNowPlayingWithCollection() {
        final Urn nowPlayingCollection = Urn.forTrackStation(123L);
        final Urn nowPlayingTrack = Urn.forTrack(123L);

        adapter.onNext(asList(searchItem,
                              playlistTagItem,
                              tracksBucketItem,
                              stationsBucketItem,
                              chartsBucketItem));

        adapter.updateNowPlayingWithCollection(nowPlayingCollection, nowPlayingTrack);

        verify(stationsBucketItem).updateNowPlaying(nowPlayingCollection);
        verify(recommendedStationsBucketRenderer).notifyAdapter();
        verify(tracksBucketItem).updateNowPlaying(nowPlayingTrack);
    }

    @Test
    public void updatesNowPlayingTrack() {
        final Urn nowPlayingTrack = Urn.forTrack(123L);

        adapter.onNext(asList(searchItem,
                              playlistTagItem,
                              tracksBucketItem,
                              stationsBucketItem,
                              chartsBucketItem));

        adapter.updateNowPlaying(nowPlayingTrack);

        verify(tracksBucketItem).updateNowPlaying(nowPlayingTrack);
        verify(stationsBucketItem, never()).updateNowPlaying(nowPlayingTrack);
    }

    @Test
    public void setItemWhenItemIsAlreadyPresent() {
        final DiscoveryItem initialItem = DiscoveryItem.forSearchItem();
        final DiscoveryItem updatedItem = DiscoveryItem.forSearchItem();
        adapter.onNext(singletonList(initialItem));

        adapter.setItem(0, updatedItem);

        assertThat(adapter.getItems()).containsExactly(updatedItem);
    }

    @Test
    public void setItemWhenItemIsAbsent() {
        final DiscoveryItem updatedItem = DiscoveryItem.forSearchItem();
        adapter.onNext(Collections.<DiscoveryItem>emptyList());

        adapter.setItem(0, updatedItem);

        assertThat(adapter.getItems()).containsExactly(updatedItem);
    }
}
