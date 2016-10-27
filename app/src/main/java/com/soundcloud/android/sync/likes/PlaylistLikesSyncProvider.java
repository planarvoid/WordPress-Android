package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class PlaylistLikesSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<LikesSyncer<ApiPlaylist>> playlistLikesSyncer;
    private final MyPlaylistLikesStateProvider myPlaylistLikesStateProvider;

    @Inject
    public PlaylistLikesSyncProvider(@Named(LikesSyncModule.PLAYLIST_LIKES_SYNCER) Provider<LikesSyncer<ApiPlaylist>> playlistLikesSyncer,
                                     MyPlaylistLikesStateProvider myPlaylistLikesStateProvider) {
        super(Syncable.PLAYLIST_LIKES);
        this.playlistLikesSyncer = playlistLikesSyncer;
        this.myPlaylistLikesStateProvider = myPlaylistLikesStateProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return playlistLikesSyncer.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return myPlaylistLikesStateProvider.hasLocalChanges();
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(1);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
