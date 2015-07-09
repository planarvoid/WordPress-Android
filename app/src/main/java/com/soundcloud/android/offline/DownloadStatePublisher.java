package com.soundcloud.android.offline;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class DownloadStatePublisher {

    private final EventBus eventBus;

    @Inject
    public DownloadStatePublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    void publishDownloadSuccessfulEvents(DownloadQueue queue, DownloadState result) {
        publishTrackDownloaded(queue, result);
        publishRelatedQueuedCollectionsAsRequested(queue, result);
    }

    void publishDownloadErrorEvents(DownloadQueue queue, DownloadState result) {
        publishTrackUnavailable(result);
        publishRelatedAndQueuedCollectionsAsRequested(queue, result);
    }

    void publishDownloadCancelEvents(DownloadQueue queue, DownloadState result) {
        publishTrackDownloadCanceled(result);
        publishCollectionsDownloadedForCancelledTrack(queue, result);
    }

    void publishNotDownloadableStateChanges(DownloadQueue queue, OfflineContentRequests requests, Urn currentDownload) {
        publishDownloadedTracksRemoved(requests, currentDownload);
        publishTracksAlreadyDownloaded(requests);

        if (!queue.getRequests().isEmpty()) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequestRemoved(queue.getRequests()));
        }
    }

    void publishDownloadsRequested(DownloadQueue queue) {
        if (!queue.getRequests().isEmpty()) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(queue.getRequests()));
        }
    }

    void publishDownloading(DownloadRequest request) {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(request));
    }

    void publishDone() {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.idle());
    }

    private void publishTracksAlreadyDownloaded(OfflineContentRequests requests) {
        if (!requests.newRestoredRequests.isEmpty()) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(requests.newRestoredRequests));
        }
    }

    private void publishDownloadedTracksRemoved(OfflineContentRequests requests, final Urn urn) {
        if (!requests.newRemovedTracks.isEmpty()) {
            final Collection<Urn> removed = Collections2.filter(requests.newRemovedTracks, notCurrentDownload(urn));
            if (!removed.isEmpty()) {
                eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                        CurrentDownloadEvent.downloadRemoved(new ArrayList<>(removed)));
            }
        }
    }

    private Predicate<Urn> notCurrentDownload(final Urn currentDownload) {
        return new Predicate<Urn>() {
            @Override
            public boolean apply(@Nullable Urn request) {
                return request != null && !request.equals(currentDownload);
            }
        };
    }

    private void publishTrackDownloaded(DownloadQueue queue, DownloadState result) {
        final List<Urn> completed = queue.getDownloaded(result);
        final boolean isLikedTrackCompleted = queue.isAllLikedTracksDownloaded(result);

        if (hasChanges(completed, isLikedTrackCompleted)) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloaded(isLikedTrackCompleted, completed));
        }
    }

    private void publishTrackUnavailable(DownloadState result) {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.unavailable(false, Arrays.asList(result.getTrack())));
    }

    private void publishRelatedQueuedCollectionsAsRequested(DownloadQueue queue, DownloadState result) {
        final List<Urn> requested = queue.getRequested(result);
        final boolean likedTrackRequested = queue.isLikedTrackRequested();

        if (hasChanges(requested, likedTrackRequested)) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloadRequested(likedTrackRequested, requested));
        }
    }

    private void publishRelatedAndQueuedCollectionsAsRequested(DownloadQueue queue, DownloadState result) {
        List<Urn> relatedPlaylists = queue.getRequestedWithOwningPlaylists(result);
        if (!relatedPlaylists.isEmpty() || result.request.inLikedTracks) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloadRequested(result.request.inLikedTracks, relatedPlaylists));
        }
    }

    private void publishTrackDownloadCanceled(DownloadState result) {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRemoved(Arrays.asList(result.getTrack())));
    }

    private void publishCollectionsDownloadedForCancelledTrack(DownloadQueue queue, DownloadState result) {
        final List<Urn> completedCollections = queue.getDownloadedPlaylists(result);
        final boolean isLikedTrackCompleted = queue.isAllLikedTracksDownloaded(result);

        if (hasChanges(completedCollections, isLikedTrackCompleted)) {
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloaded(isLikedTrackCompleted, completedCollections));
        }
    }

    private boolean hasChanges(List<Urn> entitiesChangeList, boolean likedTracksChanged) {
        return !entitiesChangeList.isEmpty() || likedTracksChanged;
    }
}
