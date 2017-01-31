package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendedPlaylistsOperations {

    private final SyncOperations syncOperations;
    private final RecommendedPlaylistsStorage storage;
    private final PlaylistRepository playlistRepository;

    @Inject
    RecommendedPlaylistsOperations(SyncOperations syncOperations,
                                   RecommendedPlaylistsStorage storage,
                                   PlaylistRepository playlistRepository) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.playlistRepository = playlistRepository;
    }

    public Observable<DiscoveryItem> recommendedPlaylists() {
        return syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)
                             .flatMap(o -> readRecommendedPlaylistsFromStorage());
    }

    public Observable<DiscoveryItem> refreshRecommendedPlaylists() {
        return syncOperations.failSafeSync(Syncable.RECOMMENDED_PLAYLISTS)
                             .flatMap(o -> readRecommendedPlaylistsFromStorage());
    }

    private Observable<RecommendedPlaylistsBucketItem> fromEntities(final List<RecommendedPlaylistsEntity> buckets) {
        final Set<Urn> recommendedPlaylists = new HashSet<>();
        for (RecommendedPlaylistsEntity bucket : buckets) {
            recommendedPlaylists.addAll(bucket.playlistUrns());
        }

        if (recommendedPlaylists.isEmpty()) {
            return Observable.empty();
        }

        return playlistRepository.withUrns(recommendedPlaylists)
                                 .flatMap(toOrderedBuckets(buckets));
    }

    private Func1<Map<Urn, Playlist>, Observable<? extends RecommendedPlaylistsBucketItem>> toOrderedBuckets(List<RecommendedPlaylistsEntity> buckets) {
        return playlistEntities -> Observable
                .from(buckets)
                .map(mapToBucketItem(playlistEntities));
    }

    private Func1<RecommendedPlaylistsEntity, RecommendedPlaylistsBucketItem> mapToBucketItem(final Map<Urn, Playlist> playlistEntities) {
        return entity -> {
            final List<PlaylistItem> matches = new ArrayList<>(entity.playlistUrns().size());
            for (Urn urn : entity.playlistUrns()) {
                matches.add(PlaylistItem.from(playlistEntities.get(urn)));
            }
            return RecommendedPlaylistsBucketItem.create(entity, matches);
        };
    }

    private Observable<? extends DiscoveryItem> readRecommendedPlaylistsFromStorage() {
        return storage.recommendedPlaylists()
                      .flatMap(new Func1<List<RecommendedPlaylistsEntity>, Observable<RecommendedPlaylistsBucketItem>>() {
                          @Override
                          public Observable<RecommendedPlaylistsBucketItem> call(List<RecommendedPlaylistsEntity> entities) {
                              return fromEntities(entities);
                          }
                      });
    }

}
