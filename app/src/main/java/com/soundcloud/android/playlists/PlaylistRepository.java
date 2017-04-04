package com.soundcloud.android.playlists;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.utils.DiffUtils.minus;
import static com.soundcloud.java.collections.Maps.asMap;
import static java.util.Collections.singletonList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlaylistRepository {

    private final PlaylistStorage playlistStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    public PlaylistRepository(PlaylistStorage playlistStorage,
                              SyncInitiator syncInitiator,
                              @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.playlistStorage = playlistStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    @SuppressWarnings("WeakerAccess")
    public Observable<Playlist> withUrn(final Urn playlistUrn) {
        final Collection<Urn> requestedPlaylists = singletonList(playlistUrn);
        return playlistStorage
                .availablePlaylists(requestedPlaylists)
                .flatMap(syncMissingPlaylists(requestedPlaylists))
                .flatMap(o -> playlistStorage.loadPlaylists(requestedPlaylists))
                .subscribeOn(scheduler)
                .filter(list -> !list.isEmpty())
                .map(playlistItems -> playlistItems.get(0));
    }

    public Observable<Map<Urn, Playlist>> withUrns(final Collection<Urn> requestedPlaylists) {
        return playlistStorage
                .availablePlaylists(requestedPlaylists)
                .flatMap(syncMissingPlaylists(requestedPlaylists))
                .flatMap(o -> playlistStorage.loadPlaylists(requestedPlaylists))
                .onErrorResumeNext(playlistStorage.loadPlaylists(requestedPlaylists))
                .map(playlistItems -> asMap(playlistItems, Playlist::urn))
                .subscribeOn(scheduler);

    }

    private Func1<List<Urn>, Observable<SyncJobResult>> syncMissingPlaylists(final Collection<Urn> requestedPlaylists) {
        return playlistsAvailable -> {
            final List<Urn> missingPlaylists = minus(requestedPlaylists, playlistsAvailable);
            if (missingPlaylists.isEmpty()) {
                return Observable.just(null);
            } else {
                return syncInitiator
                        .batchSyncPlaylists(missingPlaylists)
                        .observeOn(scheduler);
            }
        };
    }


}
