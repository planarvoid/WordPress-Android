package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.UpsellItem;
import static com.soundcloud.android.discovery.OldDiscoveryItem.Kind.WelcomeUserItem;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.OldDiscoveryAdapter.DiscoveryItemListenerBucket;
import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.discovery.newforyou.NewForYouBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksBucketItem;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsBucketRenderer;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItem;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItemRenderer;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.upsell.DiscoveryUpsellItemRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class OldDiscoveryAdapterTest extends AndroidUnitTest {

    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;
    @Mock private RecommendedStationsBucketRenderer recommendedStationsBucketRenderer;
    @Mock private RecommendedPlaylistsBucketRenderer recommendedPlaylistsBucketRenderer;
    @Mock private ChartsBucketItemRenderer chartsBucketItemRenderer;
    @Mock private RecommendationsFooterRenderer recommendationsFooterRenderer;
    @Mock private EmptyOldDiscoveryItemRenderer emptyOldDiscoveryItemRenderer;
    @Mock private WelcomeUserItemRenderer welcomeUserItemRenderer;
    @Mock private NewForYouBucketRenderer newForYouBucketRenderer;
    @Mock private RecommendedStationsBucketItem stationsBucketItem;
    @Mock private RecommendedTracksBucketItem tracksBucketItem;
    @Mock private PlaylistTagsItem playlistTagItem;
    @Mock private ChartsBucketItem chartsBucketItem;
    @Mock private WelcomeUserItem welcomeUserItem;
    @Mock private DiscoveryUpsellItemRenderer discoveryUpsellItemRenderer;


    private OldDiscoveryItem upsellItem = OldDiscoveryItem.Default.create(OldDiscoveryItem.Kind.UpsellItem);
    private OldDiscoveryItem searchItem = OldDiscoveryItem.forSearchItem();
    private OldDiscoveryAdapter adapter;


    @Before
    public void setUp() throws Exception {
        when(stationsBucketItem.getKind()).thenReturn(RecommendedStationsItem);
        when(tracksBucketItem.getKind()).thenReturn(RecommendedTracksItem);
        when(playlistTagItem.getKind()).thenReturn(PlaylistTagsItem);
        when(chartsBucketItem.getKind()).thenReturn(ChartItem);
        when(welcomeUserItem.getKind()).thenReturn(WelcomeUserItem);


        adapter = new OldDiscoveryAdapter(recommendationBucketRenderer,
                                          playlistTagRenderer,
                                          searchItemRenderer,
                                          recommendedStationsBucketRenderer,
                                          recommendedPlaylistsBucketRenderer,
                                          chartsBucketItemRenderer,
                                          recommendationsFooterRenderer,
                                          welcomeUserItemRenderer,
                                          emptyOldDiscoveryItemRenderer,
                                          newForYouBucketRenderer,
                                          discoveryUpsellItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        adapter.onNext(asList(searchItem,
                              upsellItem,
                              welcomeUserItem,
                              playlistTagItem,
                              tracksBucketItem,
                              stationsBucketItem,
                              chartsBucketItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(SearchItem.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(UpsellItem.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(WelcomeUserItem.ordinal());
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(PlaylistTagsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(4)).isEqualTo(RecommendedTracksItem.ordinal());
        assertThat(adapter.getBasicItemViewType(5)).isEqualTo(RecommendedStationsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(6)).isEqualTo(ChartItem.ordinal());
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListenerBucket itemListener = mock(DiscoveryItemListenerBucket.class);
        final DiscoveryUpsellItemRenderer.Listener upsellListener = mock(DiscoveryUpsellItemRenderer.Listener.class);
        adapter.setDiscoveryListener(itemListener);
        adapter.setUpsellItemListener(upsellListener);

        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
        verify(searchItemRenderer).setSearchListener(itemListener);
        verify(recommendedStationsBucketRenderer).setListener(itemListener);
        verify(discoveryUpsellItemRenderer).setListener(upsellListener);
    }

    @Test
    public void setItemWhenItemIsAlreadyPresent() {
        final OldDiscoveryItem initialItem = OldDiscoveryItem.forSearchItem();
        final OldDiscoveryItem updatedItem = OldDiscoveryItem.forSearchItem();
        adapter.onNext(singletonList(initialItem));

        adapter.setItem(0, updatedItem);

        assertThat(adapter.getItems()).containsExactly(updatedItem);
    }

    @Test
    public void setItemWhenItemIsAbsent() {
        final OldDiscoveryItem updatedItem = OldDiscoveryItem.forSearchItem();
        adapter.onNext(Collections.emptyList());

        adapter.setItem(0, updatedItem);

        assertThat(adapter.getItems()).containsExactly(updatedItem);
    }
}
