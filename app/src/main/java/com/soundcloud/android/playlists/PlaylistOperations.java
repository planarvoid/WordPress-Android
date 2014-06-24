package com.soundcloud.android.playlists;


import com.soundcloud.android.model.PlaylistUrn;
import com.soundcloud.android.model.TrackUrn;
import rx.Observable;

import javax.inject.Inject;

public class PlaylistOperations {

    private final PlaylistStorage playlistStorage;

    @Inject
    public PlaylistOperations(PlaylistStorage playlistStorage) {
        this.playlistStorage = playlistStorage;
    }

    public Observable<TrackUrn> trackUrnsForPlayback(PlaylistUrn playlistUrn) {
        return playlistStorage.trackUrns(playlistUrn);
    }
}
