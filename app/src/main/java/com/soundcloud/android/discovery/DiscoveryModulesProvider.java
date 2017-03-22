package com.soundcloud.android.discovery;

import com.soundcloud.android.configuration.experiments.NewForYouConfig;
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
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.android.utils.EmptyThrowable;
import com.soundcloud.android.rx.RxJava;
import rx.Observable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class DiscoveryModulesProvider {

    private static final int MAX_NEW_FOR_YOU_TRACKS = 5;
    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;
    private final FeatureFlags featureFlags;
    private final RecommendedTracksOperations recommendedTracksOperations;
    private final RecommendedStationsOperations recommendedStationsOperations;
    private final RecommendedPlaylistsOperations recommendedPlaylistsOperations;
    private final ChartsOperations chartsOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final WelcomeUserOperations welcomeUserOperations;
    private final NewForYouOperations newForYouOperations;
    private final NewForYouConfig newForYouConfig;
    private final InlineUpsellOperations inlineUpsellOperations;

    @Inject
    DiscoveryModulesProvider(PlaylistDiscoveryConfig playlistDiscoveryConfig,
                             FeatureFlags featureFlags,
                             RecommendedTracksOperations recommendedTracksOperations,
                             RecommendedStationsOperations recommendedStationsOperations,
                             RecommendedPlaylistsOperations recommendedPlaylistsOperations,
                             ChartsOperations chartsOperations,
                             PlaylistDiscoveryOperations playlistDiscoveryOperations,
                             WelcomeUserOperations welcomeUserOperations,
                             NewForYouOperations newForYouOperations,
                             NewForYouConfig newForYouConfig,
                             InlineUpsellOperations inlineUpsellOperations) {
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
        this.featureFlags = featureFlags;
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.recommendedStationsOperations = recommendedStationsOperations;
        this.recommendedPlaylistsOperations = recommendedPlaylistsOperations;
        this.chartsOperations = chartsOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.welcomeUserOperations = welcomeUserOperations;
        this.newForYouOperations = newForYouOperations;
        this.newForYouConfig = newForYouConfig;
        this.inlineUpsellOperations = inlineUpsellOperations;
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
                    newForYouFirst(isRefresh),
                    recommendedTracks(isRefresh),
                    newForYouSecond(isRefresh),
                    recommendedPlaylists(isRefresh),
                    upsell(),
                    recommendedStations(isRefresh),
                    charts(isRefresh)
            );
        }
        return Arrays.asList(
                userWelcome(isRefresh),
                newForYouFirst(isRefresh),
                recommendedTracks(isRefresh),
                newForYouSecond(isRefresh),
                recommendedStations(isRefresh),
                recommendedPlaylists(isRefresh),
                upsell(),
                charts(isRefresh)
        );
    }

    private List<Observable<DiscoveryItem>> itemsForDefault(boolean isRefresh) {
        return Arrays.asList(
                userWelcome(isRefresh),
                newForYouFirst(isRefresh),
                recommendedTracks(isRefresh),
                newForYouSecond(isRefresh),
                recommendedPlaylists(isRefresh),
                upsell(),
                recommendedStations(isRefresh),
                charts(isRefresh),
                playlistTags()
        );
    }

    private Observable<DiscoveryItem> upsell() {
        if (inlineUpsellOperations.shouldDisplayInDiscovery()) {
            return Observable.just(DiscoveryItem.Default.create(DiscoveryItem.Kind.UpsellItem));
        }
        return Observable.empty();
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
                RxJava.toV1Observable(chartsOperations.refreshFeaturedCharts()) :
                RxJava.toV1Observable(chartsOperations.featuredCharts());
    }

    private Observable<DiscoveryItem> newForYouFirst(boolean isRefresh) {
        if (!newForYouConfig.isTopPositionEnabled()) {
            return Observable.empty();
        }

        return newForYouItem(isRefresh);
    }

    private Observable<DiscoveryItem> newForYouSecond(boolean isRefresh) {
        if (!newForYouConfig.isSecondPositionEnabled()) {
            return Observable.empty();
        }

        return newForYouItem(isRefresh);
    }

    private Observable<DiscoveryItem> newForYouItem(boolean isRefresh) {
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
