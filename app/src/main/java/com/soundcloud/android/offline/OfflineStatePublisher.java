package com.soundcloud.android.offline;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.EventBus;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class OfflineStatePublisher {

    private final String TAG = OfflineStatePublisher.class.getSimpleName();
    private final EventBus eventBus;

    private OfflineContentUpdates updates;

    @Inject
    public OfflineStatePublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setUpdates(OfflineContentUpdates updates) {
        this.updates = updates;
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

    void publishNotDownloadableStateChanges(DownloadQueue queue, Urn currentDownload) {
        publishDownloadedTracksRemoved(currentDownload);
        publishTracksAlreadyDownloaded();
        publishCreatorOptOut();

        if (!queue.getRequests().isEmpty()) {
            Log.d(TAG, "downloadRequestRemoved");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequestRemoved(queue.getRequests()));
        }
    }

    private void publishCreatorOptOut() {
        if (!updates.creatorOptOutRequests.isEmpty()) {
            Log.d(TAG, "creatorOptOut");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.unavailable(updates.creatorOptOutRequests));
        }
    }

    void publishDownloadsRequested(DownloadQueue queue) {
        if (!queue.getRequests().isEmpty()) {
            Log.d(TAG, "downloadRequested");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloadRequested(queue.getRequests()));
        }
    }

    void publishDownloading(DownloadRequest request) {
        Log.d(TAG, "downloading");
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloading(request));
    }

    void publishDone() {
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.idle());
    }

    private void publishTracksAlreadyDownloaded() {
        if (!updates.newRestoredRequests.isEmpty()) {
            Log.d(TAG, "downloaded");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD, CurrentDownloadEvent.downloaded(updates.newRestoredRequests));
        }
    }

    private void publishDownloadedTracksRemoved(final Urn urn) {
        if (!updates.newRemovedTracks.isEmpty()) {
            final Collection<Urn> removed = MoreCollections.filter(updates.newRemovedTracks, notCurrentDownload(urn));
            if (!removed.isEmpty()) {
                Log.d(TAG, "downloadRemoved");
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
        final boolean isAllLikesCompleted = queue.isAllLikedTracksDownloaded(result);

        if (hasChanges(completed, isAllLikesCompleted)) {
            Log.d(TAG, "downloaded");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloaded(isAllLikesCompleted, completed));
        }
    }

    private void publishTrackUnavailable(DownloadState result) {
        Log.d(TAG, "unavailable");
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.unavailable(result.request.isLiked(), Collections.singletonList(result.getTrack())));
    }

    private void publishRelatedQueuedCollectionsAsRequested(DownloadQueue queue, DownloadState result) {
        final List<Urn> requested = queue.getRequested(result);
        final boolean likedTrackRequested = queue.isLikedTrackRequested();

        if (hasChanges(requested, likedTrackRequested)) {
            Log.d(TAG, "downloadRequested");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloadRequested(likedTrackRequested, requested));
        }
    }

    private void publishRelatedAndQueuedCollectionsAsRequested(DownloadQueue queue, DownloadState result) {
        List<Urn> relatedPlaylists = queue.getRequestedWithOwningPlaylists(result);
        if (!relatedPlaylists.isEmpty() || result.request.isLiked()) {
            Log.d(TAG, "downloadRequested");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloadRequested(result.request.isLiked(), relatedPlaylists));
        }
    }

    private void publishTrackDownloadCanceled(DownloadState result) {
        Log.d(TAG, "downloadRemoved");
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                CurrentDownloadEvent.downloadRemoved(Collections.singletonList(result.getTrack())));
    }

    private void publishCollectionsDownloadedForCancelledTrack(DownloadQueue queue, DownloadState result) {
        final List<Urn> completedCollections = queue.getDownloadedPlaylists(result);
        final boolean isLikedTrackCompleted = queue.isAllLikedTracksDownloaded(result);

        if (hasChanges(completedCollections, isLikedTrackCompleted)) {
            Log.d(TAG, "downloaded");
            eventBus.publish(EventQueue.CURRENT_DOWNLOAD,
                    CurrentDownloadEvent.downloaded(isLikedTrackCompleted, completedCollections));
        }
    }

    private boolean hasChanges(List<Urn> entitiesChangeList, boolean likedTracksChanged) {
        return !entitiesChangeList.isEmpty() || likedTracksChanged;
    }
}
