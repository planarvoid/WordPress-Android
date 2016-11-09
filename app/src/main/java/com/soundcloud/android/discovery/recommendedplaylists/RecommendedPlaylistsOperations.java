package com.soundcloud.android.discovery.recommendedplaylists;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import rx.Observable;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendedPlaylistsOperations {

    private final SyncOperations syncOperations;
    private final RecommendedPlaylistsStorage storage;
    private final PlaylistOperations playlistOperations;

    @Inject
    RecommendedPlaylistsOperations(SyncOperations syncOperations,
                                   RecommendedPlaylistsStorage storage,
                                   PlaylistOperations playlistOperations) {
        this.syncOperations = syncOperations;
        this.storage = storage;
        this.playlistOperations = playlistOperations;
    }

    public Observable<DiscoveryItem> recommendedPlaylists() {
        return syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_PLAYLISTS)
                             .flatMap(continueWith(readRecommendedPlaylistsFromStorage()));
    }

    public Observable<DiscoveryItem> refreshRecommendedPlaylists() {
        return syncOperations.failSafeSync(Syncable.RECOMMENDED_PLAYLISTS)
                             .flatMap(continueWith(readRecommendedPlaylistsFromStorage()));
    }

    private Observable<RecommendedPlaylistsBucketItem> fromEntities(final List<RecommendedPlaylistsEntity> entities) {
        Set<Urn> urns = new HashSet<>();
        for (RecommendedPlaylistsEntity entity : entities) {
            urns.addAll(entity.playlistUrns());
        }

        if (urns.isEmpty()) {
            return Observable.empty();
        }

        return playlistOperations.playlistsMap(urns)
                                 .flatMap(new Func1<Map<Urn, PlaylistItem>, Observable<RecommendedPlaylistsBucketItem>>() {
                                     @Override
                                     public Observable<RecommendedPlaylistsBucketItem> call(final Map<Urn, PlaylistItem> playlistEntities) {
                                         return Observable.from(entities)
                                                          .map(mapToBucketItem(playlistEntities));
                                     }
                                 });
    }

    @NonNull
    private Func1<RecommendedPlaylistsEntity, RecommendedPlaylistsBucketItem> mapToBucketItem(final Map<Urn, PlaylistItem> playlistEntities) {
        return new Func1<RecommendedPlaylistsEntity, RecommendedPlaylistsBucketItem>() {
            @Override
            public RecommendedPlaylistsBucketItem call(RecommendedPlaylistsEntity entity) {
                List<PlaylistItem> matches = new ArrayList<>(entity.playlistUrns().size());
                for (Urn urn : entity.playlistUrns()) {
                    matches.add(playlistEntities.get(urn));
                }
                return RecommendedPlaylistsBucketItem.create(entity, matches);

            }
        };
    }

    @NonNull
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
