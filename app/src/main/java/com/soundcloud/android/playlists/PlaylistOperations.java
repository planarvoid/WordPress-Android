package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlaylistOperations {

    private final Scheduler storageScheduler;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    private final LegacyPlaylistOperations legacyPlaylistOperations;

    @Inject
    PlaylistOperations(@Named("Storage") Scheduler scheduler, LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns,
                       LegacyPlaylistOperations legacyPlaylistOperations) {
        this.storageScheduler = scheduler;
        this.loadPlaylistTrackUrns = loadPlaylistTrackUrns;
        this.legacyPlaylistOperations = legacyPlaylistOperations;
    }

    Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return loadPlaylistTrackUrns.with(playlistUrn)
                .toObservable()
                .subscribeOn(storageScheduler);
    }

    // TODO: rewriting this we'll do as a separate step
    Observable<PlaylistInfo> playlistInfo(Urn playlistUrn) {
        return legacyPlaylistOperations.loadPlaylist(playlistUrn).map(new Func1<PublicApiPlaylist, PlaylistInfo>() {
            @Override
            public PlaylistInfo call(PublicApiPlaylist publicApiPlaylist) {
                return new PlaylistInfo(publicApiPlaylist);
            }
        });
    }

    // TODO: this will have to go through the syncer
    Observable<PlaylistInfo> updatedPlaylistInfo(Urn playlistUrn) {
        return playlistInfo(playlistUrn);
    }
}
