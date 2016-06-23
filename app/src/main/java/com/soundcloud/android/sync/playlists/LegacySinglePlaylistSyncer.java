package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.LegacySyncResult;
import com.soundcloud.android.sync.SyncStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

public class LegacySinglePlaylistSyncer implements SyncStrategy {

    private final Urn playlistUrn;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;

    public LegacySinglePlaylistSyncer(SinglePlaylistSyncerFactory singlePlaylistSyncerFactory, Urn playlistUrn) {
        this.playlistUrn = playlistUrn;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
    }

    @NotNull
    @Override
    public LegacySyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return singlePlaylistSyncerFactory.create(playlistUrn).call()
               ? LegacySyncResult.fromSuccessfulChange(uri)
               : LegacySyncResult.fromSuccessWithoutChange(uri);
    }
}
