package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.experiments.ChartsExperiment;
import com.soundcloud.android.discovery.charts.Chart;
import com.soundcloud.android.discovery.charts.ChartBucket;
import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsOperations;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.stations.StationViewModel;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DiscoveryPresenterDataSourceTest {

    private static final Function<DiscoveryItem, DiscoveryItem.Kind> TO_KIND = new Function<DiscoveryItem, DiscoveryItem.Kind>() {
        @Nullable
        @Override
        public DiscoveryItem.Kind apply(DiscoveryItem item) {
            return item.getKind();
        }
    };
    private final TestSubscriber<List<DiscoveryItem>> subscriber = new TestSubscriber<>();

    private DiscoveryPresenter.DataSource dataSource;

    @Mock private RecommendedTracksOperations recommendedTracksOperations;
    @Mock private PlaylistDiscoveryOperations playlistDiscoveryOperations;
    @Mock private RecommendedStationsOperations recommendedStationsOperations;
    @Mock private ChartsOperations chartsOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private ChartsExperiment chartsExperiment;

    @Before
    public void setUp() throws Exception {
        dataSource = new DiscoveryPresenter.DataSource(recommendedTracksOperations,
                                                       playlistDiscoveryOperations,
                                                       recommendedStationsOperations,
                                                       chartsOperations,
                                                       featureFlags,
                                                       chartsExperiment);

        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(true);
        when(chartsExperiment.isEnabled()).thenReturn(true);

        final ChartsBucketItem chartsItem = ChartsBucketItem.from(ChartBucket.create(Collections.<Chart>emptyList(),
                                                                                     Collections.<Chart>emptyList()));
        final RecommendedStationsBucketItem stationsItem = RecommendedStationsBucketItem.create(Collections.<StationViewModel>emptyList());
        final DiscoveryItem tracksItem = DiscoveryItem.Default.create(DiscoveryItem.Kind.RecommendedTracksItem);
        final PlaylistTagsItem playlistTagsItem = PlaylistTagsItem.create(Collections.singletonList("Test tag"),
                                                                          Collections.<String>emptyList());

        when(chartsOperations.featuredCharts()).thenReturn(Observable.<DiscoveryItem>just(chartsItem));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.<DiscoveryItem>just(stationsItem));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.just(tracksItem));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.<DiscoveryItem>just(playlistTagsItem));
    }

    @Test
    public void loadsAllItemsInOrderSearchStationsTracksChartsTags() {
        dataSource.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsExceptChartsWhenExperimentAndFlagAreDisabled() {
        when(chartsExperiment.isEnabled()).thenReturn(false);
        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(false);
        dataSource.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadAllItemsWithError() {
        when(chartsExperiment.isEnabled()).thenReturn(false);
        when(featureFlags.isEnabled(Flag.DISCOVERY_CHARTS)).thenReturn(false);
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));

        dataSource.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.Empty
        );
    }
}
