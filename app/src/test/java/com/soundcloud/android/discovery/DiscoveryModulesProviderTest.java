package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.experiments.DiscoveryModulesPositionExperiment;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.discovery.charts.ChartBucket;
import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsOperations;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsOperations;
import com.soundcloud.android.discovery.welcomeuser.TimeOfDay;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItem;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class DiscoveryModulesProviderTest extends AndroidUnitTest {

    private static final Function<DiscoveryItem, DiscoveryItem.Kind> TO_KIND = DiscoveryItem::getKind;
    private final TestSubscriber<List<DiscoveryItem>> subscriber = new TestSubscriber<>();

    private DiscoveryModulesProvider discoveryModulesProvider;

    @Mock private RecommendedTracksOperations recommendedTracksOperations;
    @Mock private PlaylistDiscoveryOperations playlistDiscoveryOperations;
    @Mock private PlaylistDiscoveryConfig playlistDiscoveryConfig;
    @Mock private RecommendedStationsOperations recommendedStationsOperations;
    @Mock private ChartsOperations chartsOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private RecommendedPlaylistsOperations recommendedPlaylistsOperations;
    @Mock private WelcomeUserOperations welcomeUserOperations;
    @Mock private DiscoveryModulesPositionExperiment discoveryModulesPositionExperiment;

    @Before
    public void setUp() throws Exception {
        discoveryModulesProvider = new DiscoveryModulesProvider(discoveryModulesPositionExperiment,
                                                                playlistDiscoveryConfig,
                                                                featureFlags,
                                                                recommendedTracksOperations,
                                                                recommendedStationsOperations,
                                                                recommendedPlaylistsOperations,
                                                                chartsOperations,
                                                                playlistDiscoveryOperations,
                                                                welcomeUserOperations);

        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(false);
        when(discoveryModulesPositionExperiment.isEnabled()).thenReturn(false);
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(false);

        final ChartsBucketItem chartsItem = ChartsBucketItem.from(ChartBucket.create(Collections.emptyList(),
                                                                                     Collections.emptyList()));
        final RecommendedStationsBucketItem stationsItem = RecommendedStationsBucketItem.create(Collections.emptyList());
        final DiscoveryItem tracksItem = DiscoveryItem.Default.create(DiscoveryItem.Kind.RecommendedTracksItem);
        final DiscoveryItem playlistsItem = DiscoveryItem.Default.create(DiscoveryItem.Kind.RecommendedPlaylistsItem);
        final PlaylistTagsItem playlistTagsItem = PlaylistTagsItem.create(Collections.singletonList("Test tag"),
                                                                          Collections.emptyList());
        final DiscoveryItem welcomeUserItem = WelcomeUserItem.create(ModelFixtures.profileUser(), TimeOfDay.NIGHT);

        when(chartsOperations.featuredCharts()).thenReturn(Observable.just(chartsItem));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.just(stationsItem));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.just(tracksItem));
        when(recommendedPlaylistsOperations.recommendedPlaylists()).thenReturn(Observable.just(playlistsItem));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.just(playlistTagsItem));
        when(welcomeUserOperations.welcome()).thenReturn(Observable.just(welcomeUserItem));
    }

    @Test
    public void loadsAllItemsInOrderSearchStationsTracksTags() {
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
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
    public void loadsAllItemsIncludingWelcomeWhenEnabled() {
        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.WelcomeUserItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsIncludingCharts() {
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
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
    public void loadsItemsInCorrectOrderForDiscoveryModulesPositionExperiment() {
        when(discoveryModulesPositionExperiment.isEnabled()).thenReturn(true);

        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsExceptPlaylistDiscoveryWhenNewHomeIsEnabled() {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.RecommendedPlaylistsItem,
                DiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsAllItemsExceptPlaylistDiscoveryWhenPlaylistDiscoverySwitchIsEnabled() {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);
        when(playlistDiscoveryConfig.isPlaylistDiscoveryFirst()).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.RecommendedPlaylistsItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadAllItemsWithError() {
        when(chartsOperations.featuredCharts()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.<DiscoveryItem>error(ApiRequestException.networkError(null, new IOException("whoops"))));

        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.Empty
        );
    }
}
