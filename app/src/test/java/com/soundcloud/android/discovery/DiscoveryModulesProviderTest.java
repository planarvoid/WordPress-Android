package com.soundcloud.android.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.experiments.NewForYouConfig;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.discovery.charts.ChartBucket;
import com.soundcloud.android.discovery.charts.ChartsBucketItem;
import com.soundcloud.android.discovery.charts.ChartsOperations;
import com.soundcloud.android.discovery.newforyou.NewForYou;
import com.soundcloud.android.discovery.newforyou.NewForYouDiscoveryItem;
import com.soundcloud.android.discovery.newforyou.NewForYouOperations;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsOperations;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItem;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsBucketItem;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
    @Mock private NewForYouOperations newForYouOperations;
    @Mock private NewForYouConfig newForYouConfig;
    @Mock private InlineUpsellOperations inlineUpsellOperations;

    @Before
    public void setUp() throws Exception {
        discoveryModulesProvider = new DiscoveryModulesProvider(playlistDiscoveryConfig,
                                                                featureFlags,
                                                                recommendedTracksOperations,
                                                                recommendedStationsOperations,
                                                                recommendedPlaylistsOperations,
                                                                chartsOperations,
                                                                playlistDiscoveryOperations,
                                                                welcomeUserOperations,
                                                                newForYouOperations,
                                                                newForYouConfig,
                                                                inlineUpsellOperations);

        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(false);
        when(featureFlags.isEnabled(Flag.RECOMMENDED_PLAYLISTS)).thenReturn(false);
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(false);
        when(newForYouConfig.isSecondPositionEnabled()).thenReturn(false);
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(false);

        final ChartsBucketItem chartsItem = ChartsBucketItem.from(ChartBucket.create(Collections.emptyList(),
                                                                                     Collections.emptyList()));
        final RecommendedStationsBucketItem stationsItem = RecommendedStationsBucketItem.create(Collections.emptyList());
        final DiscoveryItem tracksItem = DiscoveryItem.Default.create(DiscoveryItem.Kind.RecommendedTracksItem);
        final DiscoveryItem playlistsItem = DiscoveryItem.Default.create(DiscoveryItem.Kind.RecommendedPlaylistsItem);
        final PlaylistTagsItem playlistTagsItem = PlaylistTagsItem.create(Collections.singletonList("Test tag"),
                                                                          Collections.emptyList());
        final DiscoveryItem welcomeUserItem = WelcomeUserItem.create(ModelFixtures.user());
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), Collections.singletonList(Track.from(ModelFixtures.create(ApiTrack.class))));

        when(chartsOperations.featuredCharts()).thenReturn(Observable.just(chartsItem));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.just(stationsItem));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.just(tracksItem));
        when(recommendedPlaylistsOperations.recommendedPlaylists()).thenReturn(Observable.just(playlistsItem));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.just(playlistTagsItem));
        when(welcomeUserOperations.welcome()).thenReturn(Observable.just(welcomeUserItem));
        when(newForYouOperations.newForYou()).thenReturn(Observable.just(newForYou));
        when(inlineUpsellOperations.shouldDisplayInDiscovery()).thenReturn(true);
    }

    @Test
    public void loadsAllItemsInOrderSearchStationsTracksTags() {
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);


        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
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
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
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
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.UpsellItem,
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
                DiscoveryItem.Kind.UpsellItem,
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
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsAllItemsWithNewForYouOnTopWhenConfigEnabled() {
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.NewForYouItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsWithNewForYouInSecondPositionWhenConfigEnabled() {
        when(newForYouConfig.isSecondPositionEnabled()).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.NewForYouItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsRecommendedPlaylistItemsWhenFeatureFlagEnabled() {
        when(featureFlags.isEnabled(Flag.RECOMMENDED_PLAYLISTS)).thenReturn(true);
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.RecommendedPlaylistsItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsItemsWithoutNewForYouWhenNoTracksAvailable() {
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), Collections.emptyList());
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Observable.just(newForYou));
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.RecommendedTracksItem,
                DiscoveryItem.Kind.UpsellItem,
                DiscoveryItem.Kind.RecommendedStationsItem,
                DiscoveryItem.Kind.ChartItem,
                DiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsMaxFiveTracksInNewForYouModule() {
        List trackList = new ArrayList();
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), trackList);
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Observable.just(newForYou));
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        NewForYouDiscoveryItem discoveryItem = null;
        for (DiscoveryItem item : discoveryItems) {
            if (item.getKind() == DiscoveryItem.Kind.NewForYouItem) {
                discoveryItem = (NewForYouDiscoveryItem) item;
                break;
            }
        }
        assertThat(discoveryItem.newForYou().tracks().size()).isEqualTo(5);
    }

    @Test
    public void loadsLessThanMaxIfFewerTracksAvailable() {
        List trackList = new ArrayList();
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), trackList);
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Observable.just(newForYou));
        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        NewForYouDiscoveryItem discoveryItem = null;
        for (DiscoveryItem item : discoveryItems) {
            if (item.getKind() == DiscoveryItem.Kind.NewForYouItem) {
                discoveryItem = (NewForYouDiscoveryItem) item;
                break;
            }
        }
        assertThat(discoveryItem.newForYou().tracks().size()).isEqualTo(4);
    }

    @Test
    public void loadAllItemsWithError() {
        when(chartsOperations.featuredCharts()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(inlineUpsellOperations.shouldDisplayInDiscovery()).thenReturn(false);

        discoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<DiscoveryItem> discoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(discoveryItems, TO_KIND)).containsExactly(
                DiscoveryItem.Kind.SearchItem,
                DiscoveryItem.Kind.Empty
        );
    }
}
