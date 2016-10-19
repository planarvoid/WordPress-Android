package com.soundcloud.android.associations;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;
import com.soundcloud.android.sync.affiliations.MyFollowingsSyncerFactory;
import com.soundcloud.android.users.UserAssociationStorage;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class MyFollowingsSyncProvider extends SyncerRegistry.SyncProvider {
    private final MyFollowingsSyncerFactory syncerFactory;
    private final UserAssociationStorage userAssociationStorage;

    @Inject
    public MyFollowingsSyncProvider(MyFollowingsSyncerFactory syncerFactory,
                                    UserAssociationStorage userAssociationStorage) {
        super(Syncable.MY_FOLLOWINGS);
        this.syncerFactory = syncerFactory;
        this.userAssociationStorage = userAssociationStorage;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return syncerFactory.create(action);
    }

    @Override
    public Boolean isOutOfSync() {
        return userAssociationStorage.hasStaleFollowings();
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
