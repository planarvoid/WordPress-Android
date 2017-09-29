package com.soundcloud.android.associations;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;
import com.soundcloud.android.sync.affiliations.MyFollowingsSyncer;
import com.soundcloud.android.users.FollowingStorage;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MyFollowingsSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<MyFollowingsSyncer> syncerProvider;
    private final FollowingStorage followingStorage;

    @Inject
    MyFollowingsSyncProvider(Provider<MyFollowingsSyncer> syncerProvider,
                             FollowingStorage followingStorage) {
        super(Syncable.MY_FOLLOWINGS);
        this.syncerProvider = syncerProvider;
        this.followingStorage = followingStorage;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return syncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return followingStorage.hasStaleFollowings();
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(12);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
