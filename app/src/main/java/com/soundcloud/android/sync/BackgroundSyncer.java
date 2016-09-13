package com.soundcloud.android.sync;

import static com.soundcloud.android.sync.SyncIntentHelper.putSyncables;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.accounts.AccountOperations;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class BackgroundSyncer {
    enum Result {
        UNAUTHORIZED,
        NO_SYNC,
        SYNCING
    }

    static int[] BACKOFF_MULTIPLIERS = new int[]{1, 2, 4, 8, 12, 18, 24};

    private final AccountOperations accountOperations;
    private final Context context;
    private final BackgroundSyncResultReceiver resultReceiver;
    private final SyncStateStorage syncStateStorage;
    private final SyncerRegistry syncerRegistry;

    BackgroundSyncer(@Provided AccountOperations accountOperations,
                     @Provided SyncStateStorage syncStateStorage,
                     @Provided SyncerRegistry syncerRegistry,
                     Context context,
                     BackgroundSyncResultReceiver resultReceiver) {
        this.accountOperations = accountOperations;
        this.syncStateStorage = syncStateStorage;
        this.syncerRegistry = syncerRegistry;
        this.context = context;
        this.resultReceiver = resultReceiver;
    }

    Result sync() {
        return sync(false);
    }

    Result sync(boolean force) {
        if (!accountOperations.isUserLoggedIn()) {
            return Result.UNAUTHORIZED;
        }

        final List<Syncable> toSync = getSyncables(force);
        if (toSync.isEmpty()) {
            return Result.NO_SYNC;
        }

        final Intent syncIntent = putSyncables(new Intent(context, ApiSyncService.class), toSync)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, false)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver);

        context.startService(syncIntent);
        return Result.SYNCING;
    }

    public List<Syncable> getSyncables(boolean forceSync) {
        return forceSync ? Arrays.asList(Syncable.values()) : getStaleSyncables();
    }

    private List<Syncable> getStaleSyncables() {
        final List<Syncable> toSync = new ArrayList<>();
        for (Syncable syncable : EnumSet.complementOf(Syncable.FOREGROUND_ONLY)) {
            final SyncerRegistry.SyncProvider syncProvider = syncerRegistry.get(syncable);
            if (shouldSync(syncable, syncProvider)) {
                toSync.add(syncable);
            }
        }
        return toSync;
    }

    private boolean shouldSync(Syncable syncable, SyncerRegistry.SyncProvider syncProvider) {
        return syncProvider.isOutOfSync() || syncProvider.usePeriodicSync() && isStale(syncable,
                                                                                       syncProvider.staleTime());
    }

    private boolean isStale(Syncable syncable, long staleTime) {
        final int misses = syncStateStorage.getSyncMisses(syncable);
        final int backoffIndex = Math.min(BACKOFF_MULTIPLIERS.length - 1, misses);
        return !syncStateStorage.hasSyncedWithin(syncable, staleTime * BACKOFF_MULTIPLIERS[backoffIndex]);
    }

}
