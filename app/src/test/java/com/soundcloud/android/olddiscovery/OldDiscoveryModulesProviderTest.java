package com.soundcloud.android.olddiscovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.experiments.NewForYouConfig;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.olddiscovery.charts.ChartBucket;
import com.soundcloud.android.olddiscovery.charts.ChartsBucketItem;
import com.soundcloud.android.olddiscovery.charts.ChartsOperations;
import com.soundcloud.android.olddiscovery.newforyou.NewForYou;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouDiscoveryItem;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouOperations;
import com.soundcloud.android.olddiscovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.olddiscovery.recommendedplaylists.RecommendedPlaylistsOperations;
import com.soundcloud.android.olddiscovery.welcomeuser.WelcomeUserItem;
import com.soundcloud.android.olddiscovery.welcomeuser.WelcomeUserOperations;
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
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class OldDiscoveryModulesProviderTest extends AndroidUnitTest {

    private static final Function<OldDiscoveryItem, OldDiscoveryItem.Kind> TO_KIND = OldDiscoveryItem::getKind;
    private final TestSubscriber<List<OldDiscoveryItem>> subscriber = new TestSubscriber<>();

    private OldDiscoveryModulesProvider oldDiscoveryModulesProvider;

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
        oldDiscoveryModulesProvider = new OldDiscoveryModulesProvider(playlistDiscoveryConfig,
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
        final OldDiscoveryItem tracksItem = OldDiscoveryItem.Default.create(OldDiscoveryItem.Kind.RecommendedTracksItem);
        final OldDiscoveryItem playlistsItem = OldDiscoveryItem.Default.create(OldDiscoveryItem.Kind.RecommendedPlaylistsItem);
        final PlaylistTagsItem playlistTagsItem = PlaylistTagsItem.create(Collections.singletonList("Test tag"),
                                                                          Collections.emptyList());
        final OldDiscoveryItem welcomeUserItem = WelcomeUserItem.create(ModelFixtures.user());
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), Collections.singletonList(Track.from(ModelFixtures.create(ApiTrack.class))));

        when(chartsOperations.featuredCharts()).thenReturn(io.reactivex.Observable.just(chartsItem));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.just(stationsItem));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.just(tracksItem));
        when(recommendedPlaylistsOperations.recommendedPlaylists()).thenReturn(Observable.just(playlistsItem));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.just(playlistTagsItem));
        when(welcomeUserOperations.welcome()).thenReturn(Observable.just(welcomeUserItem));
        when(newForYouOperations.newForYou()).thenReturn(Single.just(newForYou));
        when(inlineUpsellOperations.shouldDisplayInDiscovery()).thenReturn(true);
    }

    @Test
    public void loadsAllItemsInOrderSearchStationsTracksTags() {
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);


        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsIncludingWelcomeWhenEnabled() {
        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.WelcomeUserItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsIncludingCharts() {
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsExceptPlaylistDiscoveryWhenNewHomeIsEnabled() {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.RecommendedPlaylistsItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsAllItemsExceptPlaylistDiscoveryWhenPlaylistDiscoverySwitchIsEnabled() {
        when(playlistDiscoveryConfig.isEnabled()).thenReturn(true);
        when(playlistDiscoveryConfig.isPlaylistDiscoveryFirst()).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.RecommendedPlaylistsItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsAllItemsWithNewForYouOnTopWhenConfigEnabled() {
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.NewForYouItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsAllItemsWithNewForYouInSecondPositionWhenConfigEnabled() {
        when(newForYouConfig.isSecondPositionEnabled()).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.NewForYouItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsRecommendedPlaylistItemsWhenFeatureFlagEnabled() {
        when(featureFlags.isEnabled(Flag.RECOMMENDED_PLAYLISTS)).thenReturn(true);
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.RecommendedPlaylistsItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem
        );
    }

    @Test
    public void loadsItemsWithoutNewForYouWhenNoTracksAvailable() {
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), Collections.emptyList());
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Single.just(newForYou));
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.RecommendedTracksItem,
                OldDiscoveryItem.Kind.UpsellItem,
                OldDiscoveryItem.Kind.RecommendedStationsItem,
                OldDiscoveryItem.Kind.ChartItem,
                OldDiscoveryItem.Kind.PlaylistTagsItem
        );
    }

    @Test
    public void loadsMaxFiveTracksInNewForYouModule() {
        List<Track> trackList = Lists.newArrayList();
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), trackList);
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Single.just(newForYou));
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        NewForYouDiscoveryItem discoveryItem = null;
        for (OldDiscoveryItem item : oldDiscoveryItems) {
            if (item.getKind() == OldDiscoveryItem.Kind.NewForYouItem) {
                discoveryItem = (NewForYouDiscoveryItem) item;
                break;
            }
        }
        assertThat(discoveryItem.newForYou().tracks().size()).isEqualTo(5);
    }

    @Test
    public void loadsLessThanMaxIfFewerTracksAvailable() {
        List<Track> trackList = Lists.newArrayList();
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        trackList.add(Track.from(ModelFixtures.create(ApiTrack.class)));
        final NewForYou newForYou = NewForYou.create(new Date(), Urn.forNewForYou("1"), trackList);
        when(newForYouConfig.isTopPositionEnabled()).thenReturn(true);
        when(newForYouOperations.newForYou()).thenReturn(Single.just(newForYou));
        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        NewForYouDiscoveryItem discoveryItem = null;
        for (OldDiscoveryItem item : oldDiscoveryItems) {
            if (item.getKind() == OldDiscoveryItem.Kind.NewForYouItem) {
                discoveryItem = (NewForYouDiscoveryItem) item;
                break;
            }
        }
        assertThat(discoveryItem.newForYou().tracks().size()).isEqualTo(4);
    }

    @Test
    public void loadAllItemsWithError() {
        when(chartsOperations.featuredCharts()).thenReturn(io.reactivex.Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(playlistDiscoveryOperations.playlistTags()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedStationsOperations.recommendedStations()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(recommendedTracksOperations.recommendedTracks()).thenReturn(Observable.error(ApiRequestException.networkError(null, new IOException("whoops"))));
        when(inlineUpsellOperations.shouldDisplayInDiscovery()).thenReturn(false);

        oldDiscoveryModulesProvider.discoveryItems().subscribe(subscriber);
        subscriber.assertValueCount(1);

        final List<OldDiscoveryItem> oldDiscoveryItems = subscriber.getOnNextEvents().get(0);

        assertThat(Lists.transform(oldDiscoveryItems, TO_KIND)).containsExactly(
                OldDiscoveryItem.Kind.SearchItem,
                OldDiscoveryItem.Kind.Empty
        );
    }
}
