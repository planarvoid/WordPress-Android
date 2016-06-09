package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsItem;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryOperationsTest extends AndroidUnitTest {
    private static final Function<DiscoveryItem, DiscoveryItem.Kind> TO_KIND = new Function<DiscoveryItem, DiscoveryItem.Kind>() {
        @Nullable
        @Override
        public DiscoveryItem.Kind apply(DiscoveryItem item) {
            return item.getKind();
        }
    };
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

        final DiscoveryItem chartsItem = new DiscoveryItem(DiscoveryItem.Kind.ChartItem);
        final RecommendedStationsItem stationsItem = new RecommendedStationsItem(Collections.<StationRecord>emptyList());
        final DiscoveryItem tracksItem =  new DiscoveryItem(DiscoveryItem.Kind.RecommendedTracksItem);
        final PlaylistTagsItem playlistTagsItem = new PlaylistTagsItem(Arrays.asList("Test tag"), Collections.<String>emptyList());

        when(chartsOperations.charts()).thenReturn(Observable.just(chartsItem));
        when(recommendedStationsOperations.stationsBucket()).thenReturn(Observable.<DiscoveryItem>just(stationsItem));
        when(recommendedTracksOperations.firstBucket()).thenReturn(Observable.just(tracksItem));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.<DiscoveryItem>just(playlistTagsItem));
    }

    @Test
    public void loadsAllItemsInOrderSearchChartsStationsTracksTags() {
        operations.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);


        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void cleanUpRecommendationsData() {
        operations.clearData();

        verify(recommendedTracksOperations).clearData();
        verify(playlistDiscoveryOperations).clearData();
    }

}
