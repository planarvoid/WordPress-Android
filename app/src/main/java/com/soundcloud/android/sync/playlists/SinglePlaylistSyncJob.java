package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.likes.DefaultSyncJob;

import java.util.concurrent.Callable;

public class SinglePlaylistSyncJob extends DefaultSyncJob {

    private final Urn urn;

    public SinglePlaylistSyncJob(Callable<Boolean> syncer, Urn urn) {
        super(syncer, Syncable.PLAYLIST);
        this.urn = urn;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && urn.equals(((SinglePlaylistSyncJob) o).urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }
}
