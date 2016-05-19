package com.soundcloud.android.stations;

import com.soundcloud.android.sync.SingleJobRequest;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

import javax.inject.Inject;

public class StationsSyncRequestFactory {
    private final RecentStationsSyncer recentStationsSyncer;

    private final EventBus eventBus;

    @Inject
    public StationsSyncRequestFactory(RecentStationsSyncer recentStationsSyncer,
                                      EventBus eventBus) {
        this.recentStationsSyncer = recentStationsSyncer;
        this.eventBus = eventBus;
    }

    public SyncRequest create(String action, ResultReceiver resultReceiver) {
        switch (action) {
            case Actions.SYNC_RECENT_STATIONS:
                return new SingleJobRequest(
                        new DefaultSyncJob(recentStationsSyncer),
                        Actions.SYNC_RECENT_STATIONS,
                        true,
                        resultReceiver,
                        eventBus
                );
            default:
                throw new IllegalArgumentException("Unknown action. " + action);
        }
    }

    public static class Actions {
        public static final String SYNC_RECENT_STATIONS = "syncRecentStations";
    }
}
