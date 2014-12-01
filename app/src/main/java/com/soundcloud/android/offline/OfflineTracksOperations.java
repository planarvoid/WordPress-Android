package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;

import android.content.Context;

import javax.inject.Inject;
import java.util.List;

public class OfflineTracksOperations {

    private final TrackDownloadsStorage trackDownloadStorage;
    private final Context context;

    @Inject
    public OfflineTracksOperations(TrackDownloadsStorage trackDownloadStorage, Context context) {
        this.trackDownloadStorage = trackDownloadStorage;
        this.context = context;
    }

    public void enqueueTracks(List<Urn> tracks) {
        trackDownloadStorage.storeRequestedDownloads(tracks);
        OfflineContentService.syncOfflineContent(context);
    }

}
