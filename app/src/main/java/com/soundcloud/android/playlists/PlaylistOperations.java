package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlaylistOperations {

    private final Scheduler storageScheduler;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;

    @Inject
    PlaylistOperations(@Named("Storage") Scheduler scheduler, LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns) {
        this.storageScheduler = scheduler;
        this.loadPlaylistTrackUrns = loadPlaylistTrackUrns;
    }

    Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return loadPlaylistTrackUrns.with(playlistUrn)
                .toObservable()
                .subscribeOn(storageScheduler);
    }
}
