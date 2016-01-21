package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableList;

import javax.inject.Inject;
import java.util.Collection;
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
        return unmodifiableList(queue);
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

}
