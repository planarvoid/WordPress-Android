package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

public class OfflineContentUpdates {

    public final List<DownloadRequest> allDownloadRequests;
    public final List<DownloadRequest> newDownloadRequests;
    public final List<DownloadRequest> newRestoredRequests;
    public final List<DownloadRequest> creatorOptOutRequests;
    public final List<Urn> newRemovedTracks;

    public OfflineContentUpdates(List<DownloadRequest> allDownloadRequests,
                                 List<DownloadRequest> newDownloadRequests,
                                 List<DownloadRequest> newRestoredRequests,
                                 List<DownloadRequest> creatorOptOutRequests,
                                 List<Urn> newRemovedTracks) {
        this.allDownloadRequests = unmodifiableList(allDownloadRequests);
        this.newDownloadRequests = unmodifiableList(newDownloadRequests);
        this.newRestoredRequests = unmodifiableList(newRestoredRequests);
        this.creatorOptOutRequests = unmodifiableList(creatorOptOutRequests);
        this.newRemovedTracks = unmodifiableList(newRemovedTracks);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("allDownloads", allDownloadRequests)
                .add("newDownloads", newDownloadRequests)
                .add("newRestoredRequests", newRestoredRequests)
                .add("creatorOptOutRequests", creatorOptOutRequests)
                .add("newRemovedTracks", newRemovedTracks)
                .toString();
    }
}
