package com.soundcloud.android.playlists;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;
import static com.soundcloud.android.utils.DiffUtils.minus;
import static com.soundcloud.java.collections.Maps.asMap;
import static java.util.Collections.singletonList;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.RepositoryMissedSyncEvent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlaylistRepository {

    private final PlaylistStorage playlistStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;
    private final EventBusV2 eventBus;

    @Inject
    public PlaylistRepository(PlaylistStorage playlistStorage,
                              SyncInitiator syncInitiator,
                              @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                              EventBusV2 eventBus) {
        this.playlistStorage = playlistStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
    }

    @SuppressWarnings("WeakerAccess")
    public Maybe<Playlist> withUrn(final Urn playlistUrn) {
        final Collection<Urn> requestedPlaylists = singletonList(playlistUrn);
        return playlistStorage
                .availablePlaylists(requestedPlaylists)
                .flatMap(syncMissingPlaylist(playlistUrn))
                .flatMap(o -> playlistStorage.loadPlaylists(requestedPlaylists))
                .subscribeOn(scheduler)
                .filter(list -> !list.isEmpty())
                .map(playlistItems -> playlistItems.get(0))
                .switchIfEmpty(logEmpty());
    }

    @NonNull
    private Function<List<Urn>, SingleSource<? extends Boolean>> syncMissingPlaylist(Urn playlistUrn) {
        return playlistsAvailable -> {
            if (playlistsAvailable.contains(playlistUrn)) {
                return Single.just(true);
            } else {
                return syncInitiator.syncPlaylist(playlistUrn).map(SyncJobResult::wasSuccess).observeOn(scheduler);
            }
        };
    }

    public Single<Map<Urn, Playlist>> withUrns(final Collection<Urn> requestedPlaylists) {
        return playlistStorage
                .availablePlaylists(requestedPlaylists)
                .flatMap(syncMissingPlaylists(requestedPlaylists))
                .flatMap(o -> playlistStorage.loadPlaylists(requestedPlaylists))
                .onErrorResumeNext(playlistStorage.loadPlaylists(requestedPlaylists))
                .map(playlistItems -> asMap(playlistItems, Playlist::urn))
                .doOnSuccess(loadedPlaylists -> reportMissingAfterSync(loadedPlaylists.values().size(), requestedPlaylists.size()))
                .subscribeOn(scheduler);
    }

    public Single<List<Playlist>> loadPlaylistsByUrn(final Collection<Urn> requestedPlaylists) {
        return playlistStorage
                .availablePlaylists(requestedPlaylists)
                .flatMap(syncMissingPlaylists(requestedPlaylists))
                .flatMap(o -> playlistStorage.loadPlaylists(requestedPlaylists))
                .onErrorResumeNext(playlistStorage.loadPlaylists(requestedPlaylists))
                .doOnSuccess(loadedPlaylists -> reportMissingAfterSync(loadedPlaylists.size(), requestedPlaylists.size()))
                .subscribeOn(scheduler);
    }

    private Function<List<Urn>, Single<Boolean>> syncMissingPlaylists(final Collection<Urn> requestedPlaylists) {
        return playlistsAvailable -> {
            final List<Urn> missingPlaylists = minus(requestedPlaylists, playlistsAvailable);
            if (missingPlaylists.isEmpty()) {
                return Single.just(true);
            } else {
                return syncInitiator.batchSyncPlaylists(missingPlaylists).map(SyncJobResult::wasSuccess).observeOn(scheduler);
            }
        };
    }

    private <T> MaybeSource<? extends T> logEmpty() {
        return Maybe.<T>empty().doOnComplete(() -> logMissing(1));
    }

    private void reportMissingAfterSync(int loadedCount, int requestedCount) {
        logMissing(requestedCount - loadedCount);
    }

    private void logMissing(int missingEntities) {
        if (missingEntities > 0) {
            eventBus.publish(EventQueue.TRACKING, RepositoryMissedSyncEvent.Companion.fromPlaylistsMissing(missingEntities));
        }
    }
}
