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

    List<Urn> getRequested(DownloadState result) {
        return getIntersectionWith(result);
    }

    List<Urn> getRequestedWithOwningPlaylists(DownloadState result) {
        final List<Urn> requestedAndRelated = getRequestedEntities();
        addAllRemovingDuplication(requestedAndRelated, result.request.getPlaylists());
        requestedAndRelated.add(result.getTrack());
        return requestedAndRelated;
    }

    private List<Urn> getIntersectionWith(DownloadState result) {
        final List<Urn> stillRequested = new ArrayList<>(getRequestedEntities());
        stillRequested.retainAll(result.request.getPlaylists());
        return stillRequested;
    }

    List<Urn> getDownloaded(DownloadState result) {
        return getComplementWith(result);
    }

    List<Urn> getDownloadedPlaylists(DownloadState result) {
        final ArrayList<Urn> completed = new ArrayList<>(result.request.getPlaylists());
        completed.removeAll(getRequestedEntities());
        return completed;
    }

    private List<Urn> getComplementWith(DownloadState result) {
        final ArrayList<Urn> completed = new ArrayList<>(result.request.getPlaylists());
        completed.removeAll(getRequestedEntities());
        completed.add(result.getTrack());
        return completed;
    }

    boolean isAllLikedTracksDownloaded(DownloadState result) {
        return result.request.isLiked() && !isLikedTrackRequested();
    }

    List<Urn> getRequestedEntities() {
        final List<Urn> requested = new ArrayList<>();
        for (DownloadRequest request : queue) {
            addAllRemovingDuplication(requested, request.getPlaylists());
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
            if (pending.isLiked()) {
                return true;
            }
        }
        return false;
    }
}