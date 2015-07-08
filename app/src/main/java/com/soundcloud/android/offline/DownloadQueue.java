package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

final class DownloadQueue {
    private final LinkedList<DownloadRequest> queue;

    @Inject
    DownloadQueue() {
        queue = new LinkedList<>();
    }

    int size() {
        return queue.size();
    }

    List<DownloadRequest> getRequests() {
        return Collections.unmodifiableList(queue);
    }

    void set(Collection<DownloadRequest> requests) {
        queue.clear();
        queue.addAll(requests);
    }

    DownloadRequest poll() {
        return queue.poll();
    }

    DownloadRequest getFirst() {
        return queue.getFirst();
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    List<Urn> getRequested(DownloadResult result) {
        return getIntersectionWith(result);
    }

    List<Urn> getRequestedWithOwningPlaylists(DownloadResult result) {
        final List<Urn> requestedAndRelated = getRequestedEntities();
        addAllRemovingDuplication(requestedAndRelated, result.request.inPlaylists);

        return requestedAndRelated;
    }

    private List<Urn> getIntersectionWith(DownloadResult result) {
        final List<Urn> stillRequested = new ArrayList<>(getRequestedEntities());
        stillRequested.retainAll(result.request.inPlaylists);
        return stillRequested;
    }

    List<Urn> getDownloaded(DownloadResult result) {
        return getComplementWith(result);
    }

    List<Urn> getDownloadedPlaylists(DownloadResult result) {
        final ArrayList<Urn> completed = new ArrayList<>(result.request.inPlaylists);
        completed.removeAll(getRequestedEntities());
        return completed;
    }

    private List<Urn> getComplementWith(DownloadResult result) {
        final ArrayList<Urn> completed = new ArrayList<>(result.request.inPlaylists);
        completed.removeAll(getRequestedEntities());
        completed.add(result.getTrack());
        return completed;
    }

    boolean isAllLikedTracksDownloaded(DownloadResult result) {
        return result.request.inLikedTracks && !isLikedTrackRequested();
    }

    List<Urn> getRequestedEntities() {
        final List<Urn> requested = new ArrayList<>();
        for (DownloadRequest request : queue) {
            addAllRemovingDuplication(requested, request.inPlaylists);
        }
        return requested;
    }

    private List<Urn> addAllRemovingDuplication(List<Urn> to, List<Urn> from) {
        for (Urn urn : from) {
            if (!to.contains(urn)) {
                to.add(urn);
            }
        }
        return to;
    }

    boolean isLikedTrackRequested() {
        for (DownloadRequest pending : queue) {
            if (pending.inLikedTracks) {
                return true;
            }
        }
        return false;
    }
}