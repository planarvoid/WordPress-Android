package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MyPlaylistsSyncProvider extends SyncerRegistry.SyncProvider {

    private final MyPlaylistsSyncerFactory myPlaylistsSyncerFactory;
    private final PlaylistStorage playlistStorage;

    @Inject
    public MyPlaylistsSyncProvider(MyPlaylistsSyncerFactory myPlaylistsSyncerFactory, PlaylistStorage playlistStorage) {
        super(Syncable.MY_PLAYLISTS);
        this.myPlaylistsSyncerFactory = myPlaylistsSyncerFactory;
        this.playlistStorage = playlistStorage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return myPlaylistsSyncerFactory.create(isUiRequest);
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
