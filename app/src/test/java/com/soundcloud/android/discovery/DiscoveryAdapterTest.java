package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryAdapter.DiscoveryItemListenerBucket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

public class DiscoveryAdapterTest extends AndroidUnitTest {

    @Mock private RecommendationBucketRenderer recommendationBucketRenderer;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;
    @Mock private RecommendedStationsBucketRenderer recommendedStationsBucketRenderer;
    @Mock private ChartsItemRenderer chartsItemRenderer;

    private DiscoveryAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new DiscoveryAdapter(recommendationBucketRenderer, playlistTagRenderer, searchItemRenderer, recommendedStationsBucketRenderer, chartsItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        SearchItem searchItem = mock(SearchItem.class);
        when(searchItem.getKind()).thenReturn(SearchItem.Kind.SearchItem);
        DiscoveryItem playlistTagItem = mock(DiscoveryItem.class);
        when(playlistTagItem.getKind()).thenReturn(DiscoveryItem.Kind.PlaylistTagsItem);
        DiscoveryItem trackRecommendationItem = mock(DiscoveryItem.class);
        when(trackRecommendationItem.getKind()).thenReturn(DiscoveryItem.Kind.TrackRecommendationItem);
        DiscoveryItem stationRecommendationItem = mock(DiscoveryItem.class);
        when(stationRecommendationItem.getKind()).thenReturn(DiscoveryItem.Kind.StationRecommendationItem);
        DiscoveryItem chartRecommendationItem = mock(DiscoveryItem.class);
        when(chartRecommendationItem.getKind()).thenReturn(DiscoveryItem.Kind.ChartItem);

        adapter.onNext(Arrays.asList(searchItem, playlistTagItem, trackRecommendationItem, stationRecommendationItem, chartRecommendationItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(DiscoveryAdapter.SEARCH_TYPE);
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(DiscoveryAdapter.PLAYLIST_TAGS_TYPE);
        assertThat(adapter.getBasicItemViewType(2)).isEqualTo(DiscoveryAdapter.RECOMMENDATION_SEED_TYPE);
        assertThat(adapter.getBasicItemViewType(3)).isEqualTo(DiscoveryAdapter.STATIONS_TYPE);
        assertThat(adapter.getBasicItemViewType(4)).isEqualTo(DiscoveryAdapter.CHART_TYPE);
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListenerBucket itemListener = mock(DiscoveryItemListenerBucket.class);
        adapter.setDiscoveryListener(itemListener);

        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
    }
}
