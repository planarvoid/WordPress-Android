package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

class OfflineSyncTracker {

    private final EventBus eventBus;

    @Inject
    OfflineSyncTracker(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void downloadStarted(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromStarted(request.getTrack(), request.getTrackingData()));
    }

    public void downloadComplete(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromCompleted(request.getTrack(), request.getTrackingData()));
    }

    public void downloadCancelled(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromCancelled(request.getTrack(), request.getTrackingData()));
    }

    public void downloadFailed(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromFailed(request.getTrack(), request.getTrackingData()));
    }
}
