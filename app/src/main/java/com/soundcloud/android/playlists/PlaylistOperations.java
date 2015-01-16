package com.soundcloud.android.playlists;


import com.soundcloud.android.model.Urn;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class PlaylistOperations {

    private final PlaylistStorage playlistStorage;

    @Inject
    public PlaylistOperations(PlaylistStorage playlistStorage) {
        this.playlistStorage = playlistStorage;
    }

    public Observable<List<Urn>> trackUrnsForPlayback(Urn playlistUrn) {
        return playlistStorage.trackUrns(playlistUrn).toList();
    }
}
