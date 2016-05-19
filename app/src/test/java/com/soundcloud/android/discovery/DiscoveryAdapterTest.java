package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryAdapter.DiscoveryItemListenerBucket;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.StationRecommendationItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.TrackRecommendationItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock private RecommendationBucketRendererFactory recommendationBucketRendererFactory;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;
    @Mock private RecommendedStationsBucketRenderer recommendedStationsBucketRenderer;
    @Mock private ChartsItemRenderer chartsItemRenderer;

    private DiscoveryAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new DiscoveryAdapter(Screen.RECOMMENDATIONS_MAIN, recommendationBucketRendererFactory, playlistTagRenderer, searchItemRenderer, recommendedStationsBucketRenderer, chartsItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        SearchItem searchItem = mock(SearchItem.class);
        when(searchItem.getKind()).thenReturn(SearchItem);
        DiscoveryItem playlistTagItem = mock(DiscoveryItem.class);
        when(playlistTagItem.getKind()).thenReturn(PlaylistTagsItem);
        DiscoveryItem trackRecommendationItem = mock(DiscoveryItem.class);
        when(trackRecommendationItem.getKind()).thenReturn(TrackRecommendationItem);
        DiscoveryItem stationRecommendationItem = mock(DiscoveryItem.class);
        when(stationRecommendationItem.getKind()).thenReturn(StationRecommendationItem);
        DiscoveryItem chartRecommendationItem = mock(DiscoveryItem.class);
        when(chartRecommendationItem.getKind()).thenReturn(ChartItem);

        adapter.onNext(Arrays.asList(searchItem, playlistTagItem, trackRecommendationItem, stationRecommendationItem, chartRecommendationItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(SearchItem.ordinal());
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(PlaylistTagsItem.ordinal());
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(TrackRecommendationItem.ordinal());
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(StationRecommendationItem.ordinal());
        assertThat(adapter.getBasicItemViewType(4)).isEqualTo(ChartItem.ordinal());
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListenerBucket itemListener = mock(DiscoveryItemListenerBucket.class);
        adapter.setDiscoveryListener(itemListener);

        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
    }
}
