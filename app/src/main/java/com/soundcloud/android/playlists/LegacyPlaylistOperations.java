package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.sync.SyncInitiator;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class LegacyPlaylistOperations {

    private final PlaylistStorage playlistStorage;
    private final SyncInitiator syncInitiator;

    @Inject
    public LegacyPlaylistOperations(PlaylistStorage playlistStorage,
                                    SyncInitiator syncInitiator) {
        this.playlistStorage = playlistStorage;
        this.syncInitiator = syncInitiator;
    }

    public Observable<PublicApiPlaylist> addTrackToPlaylist(final long playlistId, final long trackId) {
        return playlistStorage.loadPlaylistAsync(playlistId).map(new Func1<PublicApiPlaylist, PublicApiPlaylist>() {
            @Override
            public PublicApiPlaylist call(PublicApiPlaylist playlist) {
                return playlistStorage.addTrackToPlaylist(playlist, trackId);
            }
        }).doOnCompleted(syncInitiator.requestSystemSyncAction());
    }

}