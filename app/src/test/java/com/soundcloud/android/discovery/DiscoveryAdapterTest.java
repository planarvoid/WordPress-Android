package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.WelcomeUserItem;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.DiscoveryAdapter.DiscoveryItemListenerBucket;
import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksBucketItem;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsBucketRenderer;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItem;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItemRenderer;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;
    @Mock private RecommendedStationsBucketRenderer recommendedStationsBucketRenderer;
    @Mock private RecommendedPlaylistsBucketRenderer recommendedPlaylistsBucketRenderer;
    @Mock private ChartsBucketItemRenderer chartsBucketItemRenderer;
    @Mock private RecommendationsFooterRenderer recommendationsFooterRenderer;
    @Mock private EmptyDiscoveryItemRenderer emptyDiscoveryItemRenderer;
    @Mock private WelcomeUserItemRenderer welcomeUserItemRenderer;
    @Mock private RecommendedStationsBucketItem stationsBucketItem;
    @Mock private RecommendedTracksBucketItem tracksBucketItem;
    @Mock private PlaylistTagsItem playlistTagItem;
    @Mock private ChartsBucketItem chartsBucketItem;
    @Mock private WelcomeUserItem welcomeUserItem;

    private DiscoveryItem searchItem = DiscoveryItem.forSearchItem();
    private DiscoveryAdapter adapter;


    @Before
    public void setUp() throws Exception {
        when(stationsBucketItem.getKind()).thenReturn(RecommendedStationsItem);
        when(tracksBucketItem.getKind()).thenReturn(RecommendedTracksItem);
        when(playlistTagItem.getKind()).thenReturn(PlaylistTagsItem);
        when(chartsBucketItem.getKind()).thenReturn(ChartItem);
        when(welcomeUserItem.getKind()).thenReturn(WelcomeUserItem);


        adapter = new DiscoveryAdapter(recommendationBucketRenderer,
                                       playlistTagRenderer,
                                       searchItemRenderer,
                                       recommendedStationsBucketRenderer,
                                       recommendedPlaylistsBucketRenderer,
                                       chartsBucketItemRenderer,
                                       recommendationsFooterRenderer,
                                       welcomeUserItemRenderer,
                                       emptyDiscoveryItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem,
                              welcomeUserItem,
                              playlistTagItem,
                              tracksBucketItem,
                              stationsBucketItem,
                              chartsBucketItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(SearchItem.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(WelcomeUserItem.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(PlaylistTagsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(RecommendedTracksItem.ordinal());
        assertThat(adapter.getBasicItemViewType(4)).isEqualTo(RecommendedStationsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(5)).isEqualTo(ChartItem.ordinal());
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListenerBucket itemListener = mock(DiscoveryItemListenerBucket.class);
        adapter.setDiscoveryListener(itemListener);

        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
        verify(searchItemRenderer).setSearchListener(itemListener);
        verify(recommendedStationsBucketRenderer).setListener(itemListener);
    }
}
