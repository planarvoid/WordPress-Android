package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.discovery.DiscoveryItem;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class RecommendedPlaylistsOperations {

    private static final Func1<RecommendedPlaylists, DiscoveryItem> TO_DISCOVERY_ITEM = new Func1<RecommendedPlaylists, DiscoveryItem>() {
        @Override
        public DiscoveryItem call(RecommendedPlaylists recommendedPlaylists) {
            return RecommendedPlaylistsBucketItem.create(recommendedPlaylists);
        }
    };

    @Inject
    public RecommendedPlaylistsOperations() {
    }

    public Observable<DiscoveryItem> recommendedPlaylists() {
        return Observable.<RecommendedPlaylists>empty().map(TO_DISCOVERY_ITEM);
    }
}
