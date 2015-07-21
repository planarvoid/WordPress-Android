package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableList;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

import java.util.List;

public class OfflineContentRequests {

    public final List<DownloadRequest> allDownloadRequests;
    public final List<DownloadRequest> newDownloadRequests;
    public final List<DownloadRequest> newRestoredRequests;
    public final List<Urn> newRemovedTracks;

    public OfflineContentRequests(List<DownloadRequest> allDownloadRequests,
                                  List<DownloadRequest> newDownloadRequests,
                                  List<DownloadRequest> newRestoredRequests,
                                  List<Urn> newRemovedTracks) {
        this.allDownloadRequests = unmodifiableList(allDownloadRequests);
        this.newDownloadRequests = unmodifiableList(newDownloadRequests);
        this.newRestoredRequests = unmodifiableList(newRestoredRequests);
        this.newRemovedTracks = unmodifiableList(newRemovedTracks);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("allDownloads", allDownloadRequests)
                .add("newDownloads", newDownloadRequests)
                .add("newRestoredRequests", newRestoredRequests)
                .add("newRemovedTracks", newRemovedTracks)
                .toString();
    }
}
