package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SuggestedCreatorsSyncProvider extends SyncerRegistry.SyncProvider {
    private final Provider<SuggestedCreatorsSyncer> suggestedCreatorsSyncerProvider;

    @Inject
    protected SuggestedCreatorsSyncProvider(Provider<SuggestedCreatorsSyncer> suggestedCreatorsSyncerProvider) {
        super(Syncable.SUGGESTED_CREATORS);
        this.suggestedCreatorsSyncerProvider = suggestedCreatorsSyncerProvider;
    }

    @Override
    public Callable<Boolean> syncer(String action, boolean isUiRequest) {
        return suggestedCreatorsSyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        return false;
    }

    @Override
    public long staleTime() {
        return TimeUnit.DAYS.toMillis(1);
    }

    @Override
    public boolean usePeriodicSync() {
        return false;
    }
}
