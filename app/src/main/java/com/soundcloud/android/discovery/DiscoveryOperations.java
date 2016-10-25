package com.soundcloud.android.discovery;

import com.soundcloud.android.discovery.charts.ChartsOperations;
import com.soundcloud.android.discovery.recommendations.RecommendedTracksOperations;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;

import javax.inject.Inject;

public class DiscoveryOperations {


    private final RecommendedTracksOperations recommendedTracksOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final RecommendedStationsOperations stationsOperations;
    private final ChartsOperations chartsOperations;

    @Inject
    DiscoveryOperations(RecommendedTracksOperations recommendedTracksOperations,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        RecommendedStationsOperations stationsOperations,
                        ChartsOperations chartsOperations) {
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.stationsOperations = stationsOperations;
        this.chartsOperations = chartsOperations;
    }

    public void clearData() {
        chartsOperations.clearData();
        stationsOperations.clearData();
        recommendedTracksOperations.clearData();
        playlistDiscoveryOperations.clearData();
    }

}
