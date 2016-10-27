package com.soundcloud.android.sync.posts;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TrackPostsSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<PostsSyncer> trackPostsSyncer;

    @Inject
    public TrackPostsSyncProvider(@Named(PostsSyncModule.MY_TRACK_POSTS_SYNCER) Provider<PostsSyncer> trackPostsSyncer) {
        super(Syncable.TRACK_POSTS);
        this.trackPostsSyncer = trackPostsSyncer;
    }



    @SuppressWarnings("unchecked")
    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return trackPostsSyncer.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
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
