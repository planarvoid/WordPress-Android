package com.soundcloud.android.sync.posts;

import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class PlaylistPostsSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<PostsSyncer> playlistPostsSyncer;
    private final PlaylistStorage playlistStorage;

    @Inject
    public PlaylistPostsSyncProvider(@Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER) Provider<PostsSyncer> playlistPostsSyncer,
                                     PlaylistStorage playlistStorage) {
        super(Syncable.PLAYLIST_POSTS);
        this.playlistPostsSyncer = playlistPostsSyncer;
        this.playlistStorage = playlistStorage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer() {
        return playlistPostsSyncer.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return playlistStorage.hasLocalChanges();
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
