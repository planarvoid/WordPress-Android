package com.soundcloud.android.stream;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;
import com.soundcloud.android.sync.stream.SoundStreamSyncer.SoundStreamSyncerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SoundStreamSyncProvider extends SyncerRegistry.SyncProvider {
    private final SoundStreamSyncerFactory syncerFactory;

    @Inject
    public SoundStreamSyncProvider(SoundStreamSyncerFactory syncerFactory) {
        super(Syncable.SOUNDSTREAM);
        this.syncerFactory = syncerFactory;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return syncerFactory.create(action);
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.MINUTES.toMillis(10);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
