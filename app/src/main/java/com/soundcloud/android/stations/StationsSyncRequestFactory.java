package com.soundcloud.android.stations;

import com.soundcloud.android.sync.SingleJobRequest;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class StationsSyncRequestFactory {
    private final RecentStationsSyncer recentStationsSyncer;
    private final RecommendedStationsSyncer recommendedStationsSyncer;

    private final EventBus eventBus;

    @Inject
    public StationsSyncRequestFactory(RecentStationsSyncer recentStationsSyncer,
                                      RecommendedStationsSyncer recommendedStationsSyncer,
                                      EventBus eventBus) {
        this.recentStationsSyncer = recentStationsSyncer;
        this.recommendedStationsSyncer = recommendedStationsSyncer;
        this.eventBus = eventBus;

    }

    public SyncRequest create(String action, ResultReceiver resultReceiver) {
        switch (action) {
            case Actions.SYNC_RECENT_STATIONS:
                return buildRequest(resultReceiver, recentStationsSyncer, action);
            case Actions.SYNC_RECOMMENDED_STATIONS:
                return buildRequest(resultReceiver, recommendedStationsSyncer, action);
            default:
                throw new IllegalArgumentException("Unknown action. " + action);
        }
    }

    private SingleJobRequest buildRequest(ResultReceiver resultReceiver, Callable<Boolean> syncJob, String action) {
        return new SingleJobRequest(
                new DefaultSyncJob(syncJob),
                action,
                true,
                resultReceiver,
                eventBus
        );
    }

    public static class Actions {
        public static final String SYNC_RECENT_STATIONS = "syncRecentStations";
        public static final String SYNC_RECOMMENDED_STATIONS = "syncRecommendedStations";
    }
}
