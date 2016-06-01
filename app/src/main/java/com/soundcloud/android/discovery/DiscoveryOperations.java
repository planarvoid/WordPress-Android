package com.soundcloud.android.discovery;

import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryOperations {
    private final RecommendedTracksOperations recommendedTracksOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final RecommendedStationsOperations recommendedStationsOperations;
    private final ChartsOperations chartsOperations;

    @Inject
    DiscoveryOperations(RecommendedTracksOperations recommendedTracksOperations,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        RecommendedStationsOperations recommendedStationsOperations,
                        ChartsOperations chartsOperations) {
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.recommendedStationsOperations = recommendedStationsOperations;
        this.chartsOperations = chartsOperations;
    }

    Observable<List<DiscoveryItem>> discoveryItems() {
        return Observable
                .just(
                        searchItem(),
                        chartsOperations.charts(),
                        recommendedStationsOperations.stationsBucket(),
                        recommendedTracksOperations.firstBucket(),
                        playlistDiscoveryOperations.playlistTags()
                ).toList()
                .compose(RxUtils.<DiscoveryItem>concatEagerIgnorePartialErrors())
                .toList();
    }

    public void clearData() {
        chartsOperations.clearData();
        recommendedStationsOperations.clearData();
        recommendedTracksOperations.clearData();
        playlistDiscoveryOperations.clearData();
    }

    private Observable<DiscoveryItem> searchItem() {
        return Observable.<DiscoveryItem>just(new SearchItem());
    }


}
