package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryAdapter.DiscoveryItemListener;
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

    @Mock private RecommendationItemRenderer recommendationItemRenderer;
    @Mock private PlaylistTagRenderer playlistTagRenderer;
    @Mock private SearchItemRenderer searchItemRenderer;

    private DiscoveryAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new DiscoveryAdapter(recommendationItemRenderer, playlistTagRenderer, searchItemRenderer);
    }

    @Test
    public void rendersCorrectViewTypes() {
        DiscoveryItem playlistTagItem = mock(DiscoveryItem.class);
        when(playlistTagItem.getKind()).thenReturn(DiscoveryItem.Kind.PlaylistTagsItem);
        DiscoveryItem trackRecommendationItem = mock(DiscoveryItem.class);
        when(trackRecommendationItem.getKind()).thenReturn(DiscoveryItem.Kind.TrackRecommendationItem);

        adapter.onNext(Arrays.asList(playlistTagItem, trackRecommendationItem));

        assertThat(adapter.getBasicItemViewType(0)).isEqualTo(DiscoveryAdapter.PLAYLIST_TAGS_TYPE);
        assertThat(adapter.getBasicItemViewType(1)).isEqualTo(DiscoveryAdapter.RECOMMENDATION_SEED_TYPE);
    }

    @Test
    public void setsClickListeners() {
        final DiscoveryItemListener itemListener = mock(DiscoveryItemListener.class);
        adapter.setOnRecommendationClickListener(itemListener);

        verify(recommendationItemRenderer).setOnRecommendationClickListener(itemListener);
        verify(playlistTagRenderer).setOnTagClickListener(itemListener);
    }
}