package com.soundcloud.android.discovery;

import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.discovery.charts.ChartsOperations;
import com.soundcloud.android.discovery.newforyou.NewForYou;
import com.soundcloud.android.discovery.newforyou.NewForYouDiscoveryItem;
import com.soundcloud.android.discovery.newforyou.NewForYouOperations;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsOperations;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import com.soundcloud.android.utils.EmptyThrowable;
import rx.Observable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class DiscoveryModulesProvider {

    public static final int MAX_NEW_FOR_YOU_TRACKS = 5;
    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;
    private final FeatureFlags featureFlags;
    private final RecommendedTracksOperations recommendedTracksOperations;
    private final RecommendedStationsOperations recommendedStationsOperations;
    private final RecommendedPlaylistsOperations recommendedPlaylistsOperations;
    private final ChartsOperations chartsOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final WelcomeUserOperations welcomeUserOperations;
    private final NewForYouOperations newForYouOperations;

    @Inject
    DiscoveryModulesProvider(PlaylistDiscoveryConfig playlistDiscoveryConfig,
                             FeatureFlags featureFlags,
                             RecommendedTracksOperations recommendedTracksOperations,
                             RecommendedStationsOperations recommendedStationsOperations,
                             RecommendedPlaylistsOperations recommendedPlaylistsOperations,
                             ChartsOperations chartsOperations,
                             PlaylistDiscoveryOperations playlistDiscoveryOperations,
                             WelcomeUserOperations welcomeUserOperations,
                             NewForYouOperations newForYouOperations) {
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
        this.featureFlags = featureFlags;
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.recommendedStationsOperations = recommendedStationsOperations;
        this.recommendedPlaylistsOperations = recommendedPlaylistsOperations;
        this.chartsOperations = chartsOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.welcomeUserOperations = welcomeUserOperations;
        this.newForYouOperations = newForYouOperations;
    }

    Observable<List<DiscoveryItem>> discoveryItems() {
        return items(getItems(false));
    }

    Observable<List<DiscoveryItem>> refreshItems() {
        return items(getItems(true));
    }

    private List<Observable<DiscoveryItem>> getItems(boolean isRefresh) {
        if (playlistDiscoveryConfig.isEnabled()) {
            return itemsForPlaylistDiscoveryExperiment(isRefresh);

        } else {
            return itemsForDefault(isRefresh);

        }
    }

    private List<Observable<DiscoveryItem>> itemsForPlaylistDiscoveryExperiment(boolean isRefresh) {
        if (playlistDiscoveryConfig.isPlaylistDiscoveryFirst()) {
            return Arrays.asList(
                    userWelcome(isRefresh),
                    newForYou(isRefresh),
                    recommendedTracks(isRefresh),
                    recommendedPlaylists(isRefresh),
                    recommendedStations(isRefresh),
                    charts(isRefresh)
            );
        }
        return Arrays.asList(
                userWelcome(isRefresh),
                newForYou(isRefresh),
                recommendedTracks(isRefresh),
                recommendedStations(isRefresh),
                recommendedPlaylists(isRefresh),
                charts(isRefresh)
        );
    }

    private List<Observable<DiscoveryItem>> itemsForDefault(boolean isRefresh) {
        return Arrays.asList(
                userWelcome(isRefresh),
                newForYou(isRefresh),
                recommendedTracks(isRefresh),
                recommendedPlaylists(isRefresh),
                recommendedStations(isRefresh),
                charts(isRefresh),
                playlistTags()
        );
    }

    private Observable<DiscoveryItem> userWelcome(boolean isRefresh) {
        return !isRefresh && featureFlags.isEnabled(Flag.WELCOME_USER) ?
               welcomeUserOperations.welcome() :
               Observable.empty();
    }

    private Observable<DiscoveryItem> recommendedTracks(boolean isRefresh) {
        return isRefresh ?
               recommendedTracksOperations.refreshRecommendedTracks() :
               recommendedTracksOperations.recommendedTracks();
    }

    private Observable<DiscoveryItem> recommendedStations(boolean isRefresh) {
        return isRefresh ?
               recommendedStationsOperations.refreshRecommendedStations() :
               recommendedStationsOperations.recommendedStations();
    }

    private Observable<DiscoveryItem> charts(boolean isRefresh) {
        return isRefresh ?
               chartsOperations.refreshFeaturedCharts() :
               chartsOperations.featuredCharts();
    }

    private Observable<DiscoveryItem> newForYou(boolean isRefresh) {
        if (featureFlags.isDisabled(Flag.NEW_FOR_YOU)) {
            return Observable.empty();
        }

        final Observable<NewForYou> newForYouObservable = isRefresh ?
                                                          newForYouOperations.refreshNewForYou() :
                                                          newForYouOperations.newForYou();

        return newForYouObservable.filter(newForYou -> !newForYou.tracks().isEmpty())
                                  .map(newForYou -> NewForYouDiscoveryItem.create(
                                          NewForYou.create(newForYou.lastUpdate(),
                                                           newForYou.queryUrn(),
                                                           newForYou.tracks().subList(0, Math.min(newForYou.tracks().size(), MAX_NEW_FOR_YOU_TRACKS)))));
    }

    private Observable<DiscoveryItem> recommendedPlaylists(boolean isRefresh) {
        if (featureFlags.isEnabled(Flag.RECOMMENDED_PLAYLISTS) || playlistDiscoveryConfig.isEnabled()) {
            return isRefresh ?
                   recommendedPlaylistsOperations.refreshRecommendedPlaylists() :
                   recommendedPlaylistsOperations.recommendedPlaylists();
        }
        return Observable.empty();
    }

    private Observable<DiscoveryItem> playlistTags() {
        if (featureFlags.isEnabled(Flag.RECOMMENDED_PLAYLISTS)) {
            return Observable.empty();
        }
        return playlistDiscoveryOperations.playlistTags();
    }

    private Observable<List<DiscoveryItem>> items(List<Observable<DiscoveryItem>> discoveryItems) {
        return Observable.just(discoveryItems)
                         .compose(RxUtils.concatEagerIgnorePartialErrors())
                         .defaultIfEmpty(EmptyViewItem.fromThrowable(new EmptyThrowable()))
                         .onErrorReturn(EmptyViewItem::fromThrowable)
                         .startWith(DiscoveryItem.forSearchItem())
                         .toList();
    }
}
