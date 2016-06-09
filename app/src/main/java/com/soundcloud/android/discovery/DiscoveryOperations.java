package com.soundcloud.android.discovery;

import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.search.PlaylistDiscoveryOperations;
import com.soundcloud.android.stations.RecommendedStationsOperations;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryOperations {
    private static final Func1<Throwable, DiscoveryItem> ERROR_ITEM = new Func1<Throwable, DiscoveryItem>() {
        @Override
        public DiscoveryItem call(Throwable throwable) {
            return new EmptyViewItem(throwable);
        }
    };

    private static final DiscoveryItem EMPTY_ITEM = new EmptyViewItem();
    private static final SearchItem SEARCH_ITEM = new SearchItem();

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
                        chartsOperations.charts(),
                        recommendedStationsOperations.stationsBucket(),
                        recommendedTracksOperations.firstBucket(),
                        playlistDiscoveryOperations.playlistTags()
                )
                .toList()
                .compose(RxUtils.<DiscoveryItem>concatEagerIgnorePartialErrors())
                .defaultIfEmpty(EMPTY_ITEM)
                .onErrorReturn(ERROR_ITEM)
                .startWith(SEARCH_ITEM)
                .toList();
    }

    public void clearData() {
        chartsOperations.clearData();
        recommendedStationsOperations.clearData();
        recommendedTracksOperations.clearData();
        playlistDiscoveryOperations.clearData();
    }

}
