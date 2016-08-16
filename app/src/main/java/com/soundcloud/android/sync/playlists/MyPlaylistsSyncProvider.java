package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MyPlaylistsSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<MyPlaylistsSyncer> myPlaylistsSyncer;
    private final PlaylistStorage playlistStorage;

    @Inject
    public MyPlaylistsSyncProvider(Provider<MyPlaylistsSyncer> myPlaylistsSyncer, PlaylistStorage playlistStorage) {
        super(Syncable.MY_PLAYLISTS);
        this.myPlaylistsSyncer = myPlaylistsSyncer;
        this.playlistStorage = playlistStorage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer(@Nullable String action) {
        return myPlaylistsSyncer.get();
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
