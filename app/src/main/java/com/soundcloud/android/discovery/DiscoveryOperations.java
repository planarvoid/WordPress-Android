package com.soundcloud.android.discovery;

import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.StationsOperations;
import rx.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryOperations {
    private final RecommendedTracksOperations recommendedTracksOperations;
    private final PlaylistDiscoveryOperations playlistDiscoveryOperations;
    private final StationsOperations stationsOperations;
    private final ChartsOperations chartsOperations;

    @Inject
    DiscoveryOperations(RecommendedTracksOperations recommendedTracksOperations,
                        PlaylistDiscoveryOperations playlistDiscoveryOperations,
                        StationsOperations stationsOperations,
                        ChartsOperations chartsOperations) {
        this.recommendedTracksOperations = recommendedTracksOperations;
        this.playlistDiscoveryOperations = playlistDiscoveryOperations;
        this.stationsOperations = stationsOperations;
        this.chartsOperations = chartsOperations;
    }

    Observable<List<DiscoveryItem>> discoveryItems() {
        List<Observable<DiscoveryItem>> items = new ArrayList<>();

        items.add(searchItem());
        items.add(chartsOperations.charts());
        items.add(stationsOperations.recommendations());
        items.add(recommendedTracksOperations.tracksBucket());
        items.add(playlistDiscoveryOperations.playlistTags());

        return Observable.concatEager(Observable.from(items)).toList();
    }

    public void clearData() {
        chartsOperations.clearData();
        recommendedTracksOperations.clearData();
        playlistDiscoveryOperations.clearData();
    }

    private Observable<DiscoveryItem> searchItem() {
        return Observable.<DiscoveryItem>just(new SearchItem());
    }


}
