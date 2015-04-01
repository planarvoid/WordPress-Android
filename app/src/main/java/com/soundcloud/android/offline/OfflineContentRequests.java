package com.soundcloud.android.offline;

import static java.util.Collections.unmodifiableList;

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
}
