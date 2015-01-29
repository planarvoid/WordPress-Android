package com.soundcloud.android.likes;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.offline.commands.OfflineTrackCountCommand;
import com.soundcloud.android.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class OfflineSyncEventOperations {

    private final OfflineTrackCountCommand offlineTrackCount;
    private final EventBus eventBus;

    private static final Func1<OfflineSyncEvent, Boolean> OFFLINE_SYNC_IN_PROGRESS = new Func1<OfflineSyncEvent, Boolean>() {
        @Override
        public Boolean call(OfflineSyncEvent offlineSyncEvent) {
            return offlineSyncEvent.getKind() == OfflineSyncEvent.START;
        }
    };

    private static final Func1<OfflineSyncEvent, Boolean> OFFLINE_SYNC_FINISHED_OR_IDLE = new Func1<OfflineSyncEvent, Boolean>() {
        @Override
        public Boolean call(OfflineSyncEvent offlineSyncEvent) {
            return offlineSyncEvent.getKind() == OfflineSyncEvent.STOP ||
                    offlineSyncEvent.getKind() == OfflineSyncEvent.IDLE;
        }
    };

    @Inject
    public OfflineSyncEventOperations(OfflineTrackCountCommand offlineTrackCount,
                                      EventBus eventBus) {
        this.offlineTrackCount = offlineTrackCount;
        this.eventBus = eventBus;
    }

    public Observable<OfflineSyncEvent> onStarted() {
        return eventBus.queue(EventQueue.OFFLINE_SYNC)
                .filter(OFFLINE_SYNC_IN_PROGRESS);
    }

    public Observable<Integer> onFinishedOrIdleWithDownloadedCount() {
        return eventBus.queue(EventQueue.OFFLINE_SYNC)
                .filter(OFFLINE_SYNC_FINISHED_OR_IDLE)
                .flatMap(offlineTrackCount);
    }

}
