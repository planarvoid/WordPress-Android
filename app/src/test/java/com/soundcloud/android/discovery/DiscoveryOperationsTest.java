package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

public class DiscoveryOperationsTest extends AndroidUnitTest {
    private final TestSubscriber<List<DiscoveryItem>> subscriber = new TestSubscriber<>();

    private DiscoveryOperations operations;

    @Mock private RecommendedTracksOperations recommendedTracksOperations;
    @Mock private PlaylistDiscoveryOperations playlistDiscoveryOperations;
    @Mock private RecommendedStationsOperations recommendedStationsOperations;
    @Mock private ChartsOperations chartsOperations;

    @Before
    public void setUp() throws Exception {
        operations = new DiscoveryOperations(recommendedTracksOperations,
                playlistDiscoveryOperations,
                recommendedStationsOperations,
                chartsOperations);

        when(recommendedTracksOperations.firstBucket()).thenReturn(Observable.<DiscoveryItem>empty());
        when(recommendedStationsOperations.stationsBucket()).thenReturn(Observable.<DiscoveryItem>empty());
        when(chartsOperations.charts()).thenReturn(Observable.<DiscoveryItem>empty());
    }

    @Test
    public void loadsAllItemsInOrderSearchChartsStationsTracksTags() {
        final DiscoveryItem trackRecommendationItem = initTrackRecommendationItem();
        final DiscoveryItem chartsItem = initChartsItem();
        final DiscoveryItem stationRecommendationItem = initStationsRecommendationItem();
        final DiscoveryItem playlistRecommendationItem = initPlaylistRecommendationItem();

        operations.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1); // prove that we wait for sync before fetching recommendations

        final List<List<DiscoveryItem>> onNextEvents = subscriber.getOnNextEvents();
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = onNextEvents.get(0);
        assertThat(discoveryItems).hasSize(5);

        assertSearchItem(discoveryItems.get(0));
        assertThat(discoveryItems.get(1)).isEqualTo(chartsItem);
        assertThat(discoveryItems.get(2)).isEqualTo(stationRecommendationItem);
        assertThat(discoveryItems.get(3)).isEqualTo(trackRecommendationItem);
        assertThat(discoveryItems.get(4)).isEqualTo(playlistRecommendationItem);
    }

    @Test
    public void cleanUpRecommendationsData() {
        operations.clearData();

        verify(recommendedTracksOperations).clearData();
        verify(playlistDiscoveryOperations).clearData();
    }

    private void assertSearchItem(DiscoveryItem discoveryItem) {
        assertThat(discoveryItem.getKind()).isEqualTo(DiscoveryItem.Kind.SearchItem);
    }

    private DiscoveryItem initTrackRecommendationItem() {
        final DiscoveryItem trackRecommendationItem = mock(DiscoveryItem.class);
        when(recommendedTracksOperations.firstBucket()).thenReturn(Observable.just(trackRecommendationItem));
        return trackRecommendationItem;
    }

    private DiscoveryItem initChartsItem() {
        final DiscoveryItem chartsItem = mock(DiscoveryItem.class);
        when(chartsOperations.charts()).thenReturn(Observable.just(chartsItem));
        return chartsItem;
    }

    private DiscoveryItem initStationsRecommendationItem() {
        final DiscoveryItem stationsRecommendationItem = mock(DiscoveryItem.class);
        when(recommendedStationsOperations.stationsBucket()).thenReturn(Observable.just(stationsRecommendationItem));
        return stationsRecommendationItem;
    }

    private DiscoveryItem initPlaylistRecommendationItem() {
        final DiscoveryItem playlistRecommendationItem = mock(DiscoveryItem.class);
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.just(playlistRecommendationItem));
        return playlistRecommendationItem;
    }
}
