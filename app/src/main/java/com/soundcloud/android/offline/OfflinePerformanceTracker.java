package com.soundcloud.android.offline;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

class OfflinePerformanceTracker {

    private final EventBus eventBus;

    @Inject
    OfflinePerformanceTracker(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void downloadStarted(DownloadRequest request) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromStarted(
                        request.getTrack(),
                        request.getTrackingData()));
    }

    public void downloadComplete(DownloadState downloadState) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromCompleted(
                        downloadState.getTrack(),
                        downloadState.request.getTrackingData()));
    }

    public void downloadCancelled(DownloadState downloadState) {
        eventBus.publish(EventQueue.TRACKING,
                OfflinePerformanceEvent.fromCancelled(
                        downloadState.getTrack(),
                        downloadState.request.getTrackingData()));
    }

    public void downloadFailed(DownloadState downloadState) {
        if (downloadState.isNotEnoughSpace() || downloadState.isNotEnoughMinimumSpace()) {
            eventBus.publish(EventQueue.TRACKING, OfflinePerformanceEvent.fromStorageLimit(
                    downloadState.getTrack(), downloadState.request.getTrackingData()));
        } else {
            eventBus.publish(EventQueue.TRACKING, OfflinePerformanceEvent.fromFailed(
                    downloadState.getTrack(), downloadState.request.getTrackingData()));
        }
    }
}
