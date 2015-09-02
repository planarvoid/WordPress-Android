package com.soundcloud.android.stations;

import com.soundcloud.java.collections.PropertySet;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

class StationsSyncer implements Callable<Boolean> {
    private final StationsStorage storage;

    @Inject
    public StationsSyncer(StationsStorage storage) {
        this.storage = storage;
    }

    @Override
    public Boolean call() throws Exception {
        final List<PropertySet> stations = storage.getRecentStationsToSync();
        // TODO : Currently blocked by the backend. Stay tuned.
        return false;
    }
}
