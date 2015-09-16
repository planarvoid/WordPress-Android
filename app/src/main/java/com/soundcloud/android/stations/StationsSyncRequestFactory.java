package com.soundcloud.android.stations;

import com.soundcloud.android.sync.SingleJobRequest;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

import javax.inject.Inject;

public class StationsSyncRequestFactory {
    private final StationsSyncer syncer;
    private final EventBus eventBus;

    @Inject
    public StationsSyncRequestFactory(StationsSyncer syncer, EventBus eventBus) {
        this.syncer = syncer;
        this.eventBus = eventBus;
    }

    public SyncRequest create(String action, ResultReceiver resultReceiver) {
        switch (action) {
            case Actions.ACTION_SYNC_STATIONS:
                return new SingleJobRequest(
                        new DefaultSyncJob(syncer),
                        Actions.ACTION_SYNC_STATIONS,
                        true,
                        resultReceiver,
                        eventBus
                );
            default:
                throw new IllegalArgumentException("Unknown action. " + action);
        }
    }

    public static class Actions {
        static final String ACTION_SYNC_STATIONS = "syncStations";
    }
}
