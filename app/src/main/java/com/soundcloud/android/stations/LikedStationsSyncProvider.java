package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.SyncerRegistry;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class LikedStationsSyncProvider extends SyncerRegistry.SyncProvider {

    private final Provider<LikedStationsSyncer> likedStationsSyncerProvider;
    private final StationsStorage stationsStorage;

    @Inject
    public LikedStationsSyncProvider(Provider<LikedStationsSyncer> likedStationsSyncerProvider,
                                     StationsStorage stationsStorage) {
        super(Syncable.LIKED_STATIONS);
        this.likedStationsSyncerProvider = likedStationsSyncerProvider;
        this.stationsStorage = stationsStorage;
    }

    @Override
    public Callable<Boolean> syncer(@Nullable String action, boolean isUiRequest) {
        return likedStationsSyncerProvider.get();
    }

    @Override
    public Boolean isOutOfSync() {
        List<Urn> likedStations = stationsStorage.getLocalLikedStations();
        List<Urn> unlikedStations = stationsStorage.getLocalUnlikedStations();
        return !likedStations.isEmpty() || !unlikedStations.isEmpty();
    }

    @Override
    public long staleTime() {
        return TimeUnit.HOURS.toMillis(4);
    }

    @Override
    public boolean usePeriodicSync() {
        return true;
    }
}
