package com.soundcloud.android.discovery;

import com.soundcloud.android.configuration.experiments.ChartsExperiment;
import com.soundcloud.android.configuration.experiments.DiscoveryModulesPositionExperiment;
import com.soundcloud.android.configuration.experiments.PlaylistDiscoveryConfig;
import com.soundcloud.android.discovery.charts.ChartsOperations;
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

    private final DiscoveryModulesPositionExperiment discoveryModulesPositionExperiment;
    private final ChartsExperiment chartsExperiment;
    private final PlaylistDiscoveryConfig playlistDiscoveryConfig;
    private final FeatureFlags featureFlags;
    private final RecommendedTracksOperations recommendedTracksOperations;
    private final RecommendedStationsOperations recommendedStationsOperations;
    private final RecommendedPlaylistsOperations recommendedPlaylistsOperations;
    private final ChartsOperations chartsOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final WelcomeUserOperations welcomeUserOperations;

    @Inject
    DiscoveryModulesProvider(DiscoveryModulesPositionExperiment discoveryModulesPositionExperiment,
                             ChartsExperiment chartsExperiment,
                             PlaylistDiscoveryConfig playlistDiscoveryConfig,
                             FeatureFlags featureFlags,
                             RecommendedTracksOperations recommendedTracksOperations,
                             RecommendedStationsOperations recommendedStationsOperations,
                             RecommendedPlaylistsOperations recommendedPlaylistsOperations,
                             ChartsOperations chartsOperations,
                             PlaylistDiscoveryOperations playlistDiscoveryOperations,
                             WelcomeUserOperations welcomeUserOperations) {
        this.discoveryModulesPositionExperiment = discoveryModulesPositionExperiment;
        this.chartsExperiment = chartsExperiment;
        this.playlistDiscoveryConfig = playlistDiscoveryConfig;
        this.featureFlags = featureFlags;
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.recommendedStationsOperations = recommendedStationsOperations;
        this.recommendedPlaylistsOperations = recommendedPlaylistsOperations;
        this.chartsOperations = chartsOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.welcomeUserOperations = welcomeUserOperations;
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

        } else if (discoveryModulesPositionExperiment.isEnabled()) {
            return itemsForDiscoveryModulesSwitchExperiment(isRefresh);

        } else {
            return itemsForDefault(isRefresh);

        }
    }

    private List<Observable<DiscoveryItem>> itemsForPlaylistDiscoveryExperiment(boolean isRefresh) {
        if (playlistDiscoveryConfig.isPlaylistDiscoveryFirst()) {
            return Arrays.asList(
                    userWelcome(),
                    recommendedTracks(isRefresh),
                    recommendedPlaylists(isRefresh),
                    recommendedStations(isRefresh),
                    charts(isRefresh)
            );
        }
        return Arrays.asList(
                userWelcome(),
                recommendedTracks(isRefresh),
                recommendedStations(isRefresh),
                recommendedPlaylists(isRefresh),
                charts(isRefresh)
        );
    }

    private List<Observable<DiscoveryItem>> itemsForDiscoveryModulesSwitchExperiment(boolean isRefresh) {
        return Arrays.asList(
                userWelcome(),
                recommendedTracks(isRefresh),
                recommendedStations(isRefresh),
                recommendedPlaylists(isRefresh),
                charts(isRefresh),
                playlistTags()
        );
    }

    private List<Observable<DiscoveryItem>> itemsForDefault(boolean isRefresh) {
        return Arrays.asList(
                userWelcome(),
                recommendedStations(isRefresh),
                recommendedTracks(isRefresh),
                recommendedPlaylists(isRefresh),
                charts(isRefresh),
                playlistTags()
        );
    }

    private Observable<DiscoveryItem> userWelcome() {
        return featureFlags.isEnabled(Flag.WELCOME_USER) ?
               welcomeUserOperations.welcome() :
               Observable.<DiscoveryItem>empty();
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
        if (featureFlags.isEnabled(Flag.DISCOVERY_CHARTS) || chartsExperiment.isEnabled() || playlistDiscoveryConfig.isEnabled()) {
            return isRefresh ?
                   chartsOperations.refreshFeaturedCharts() :
                   chartsOperations.featuredCharts();

        }
        return Observable.empty();
    }

    private Observable<DiscoveryItem> recommendedPlaylists(boolean isRefresh) {
        if (playlistDiscoveryConfig.isEnabled()) {
            return isRefresh ?
                   recommendedPlaylistsOperations.refreshRecommendedPlaylists() :
                   recommendedPlaylistsOperations.recommendedPlaylists();
        }
        return Observable.empty();
    }

    private Observable<DiscoveryItem> playlistTags() {
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
