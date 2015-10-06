package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncTrackingEvent;
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
                OfflineSyncTrackingEvent.fromStarted(request.getTrack(), request.getTrackingData()));
    }

    public void downloadComplete(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflineSyncTrackingEvent.fromCompleted(request.getTrack(), request.getTrackingData()));
    }

    public void downloadCancelled(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflineSyncTrackingEvent.fromCancelled(request.getTrack(), request.getTrackingData()));
    }

    public void downloadFailed(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflineSyncTrackingEvent.fromFailed(request.getTrack(), request.getTrackingData()));
    }
}
