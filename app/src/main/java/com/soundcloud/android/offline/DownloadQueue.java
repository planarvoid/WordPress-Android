package com.soundcloud.android.offline;

import static com.google.common.collect.Lists.newArrayList;

import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class DownloadQueue {
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

    boolean isEmpty() {
        return queue.isEmpty();
    }

    List<Urn> getRequested(DownloadResult result) {
        return getIntersectionWith(result);
    }

    private List<Urn> getIntersectionWith(DownloadResult result) {
        final List<Urn> stillRequested = getRequestedEntities();
        stillRequested.retainAll(result.getRequest().inPlaylists);
        return stillRequested;
    }

    List<Urn> getDownloaded(DownloadResult result) {
        return getComplementWith(result);
    }

    private List<Urn> getComplementWith(DownloadResult result) {
        final ArrayList<Urn> completed = newArrayList(result.getRequest().inPlaylists);
        completed.removeAll(getRequestedEntities());
        completed.add(result.getTrack());
        return completed;
    }

    boolean isAllLikedTracksDownloaded(DownloadResult result) {
        return result.getRequest().inLikedTracks && !isLikedTrackRequested();
    }

    List<Urn> getRequestedEntities() {
        final ArrayList<Urn> requested = newArrayList();
        for (DownloadRequest request : queue) {
            requested.addAll(request.inPlaylists);
        }
        return requested;
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
