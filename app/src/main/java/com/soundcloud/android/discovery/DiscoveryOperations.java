package com.soundcloud.android.discovery;

import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import rx.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
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
        List<Observable<DiscoveryItem>> items = new ArrayList<>();

        items.add(searchItem());
        items.add(chartsOperations.charts());
        items.add(recommendedStationsOperations.stations());
        items.add(recommendedTracksOperations.tracksBucket());
        items.add(playlistDiscoveryOperations.playlistTags());

        return Observable.concatEager(Observable.from(items)).toList();
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
