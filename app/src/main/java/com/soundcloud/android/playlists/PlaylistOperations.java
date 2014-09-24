package com.soundcloud.android.playlists;


import com.soundcloud.android.model.Urn;
import rx.Observable;

import javax.inject.Inject;

public class PlaylistOperations {

    private final PlaylistStorage playlistStorage;

    @Inject
    public PlaylistOperations(PlaylistStorage playlistStorage) {
        this.playlistStorage = playlistStorage;
    }

    public Observable<Urn> trackUrnsForPlayback(Urn playlistUrn) {
        return playlistStorage.trackUrns(playlistUrn);
    }
}
