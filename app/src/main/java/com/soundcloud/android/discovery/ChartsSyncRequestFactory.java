package com.soundcloud.android.discovery;

import com.soundcloud.android.sync.SingleJobRequest;
import com.soundcloud.android.sync.SyncRequest;
import com.soundcloud.android.sync.charts.ChartsSyncer;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

import javax.inject.Inject;

public class ChartsSyncRequestFactory {
    private final ChartsSyncer syncer;
    private final EventBus eventBus;

    @Inject
    public ChartsSyncRequestFactory(ChartsSyncer syncer, EventBus eventBus) {
        this.syncer = syncer;
        this.eventBus = eventBus;
    }

    public SyncRequest create(String action, ResultReceiver resultReceiver) {
        switch (action) {
            case Actions.SYNC_CHARTS:
                return new SingleJobRequest(
                        new DefaultSyncJob(syncer),
                        Actions.SYNC_CHARTS,
                        true,
                        resultReceiver,
                        eventBus
                );
            default:
                throw new IllegalArgumentException("Unknown action. " + action);
        }
    }

    public static class Actions {
        public static final String SYNC_CHARTS = "syncCharts";
    }
}
