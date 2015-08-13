package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.UNAVAILABLE_AT;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class StoreDownloadUpdatesCommand extends DefaultWriteStorageCommand<OfflineContentUpdates, WriteResult> {

    private final DateProvider dateProvider;

    @Inject
    protected StoreDownloadUpdatesCommand(PropellerDatabase propeller, DateProvider dateProvider) {
        super(propeller);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final OfflineContentUpdates requests) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.bulkUpsert(TrackDownloads.TABLE,
                        buildContentValuesForRemoval(requests.newRemovedTracks)));

                step(propeller.bulkUpsert(TrackDownloads.TABLE,
                        buildContentValuesForDownloaded(requests.newRestoredRequests)));

                step(propeller.bulkUpsert(TrackDownloads.TABLE,
                            buildContentValuesForPendingDownload(requests.newDownloadRequests)));

                step(propeller.bulkUpsert(TrackDownloads.TABLE,
                        buildOptOutContentValues(requests.creatorOptOutRequests)));
            }
        });
    }

    private List<ContentValues> buildOptOutContentValues(Collection<DownloadRequest> creatorOptOut) {
        List<ContentValues> contentValues = new ArrayList<>(creatorOptOut.size());
        for (DownloadRequest optOuts : creatorOptOut) {
            contentValues.add(ContentValuesBuilder.values(3)
                    .put(TrackDownloads.UNAVAILABLE_AT, dateProvider.getCurrentTime())
                    .put(TrackDownloads.REQUESTED_AT, null)
                    .put(TrackDownloads._ID, optOuts.track.getNumericId())
                    .get());
        }
        return contentValues;
    }

    private List<ContentValues> buildContentValuesForRemoval(List<Urn> removedTracks) {
        List<ContentValues> contentValues = new ArrayList<>(removedTracks.size());
        for (Urn track : removedTracks) {
            contentValues.add(ContentValuesBuilder
                    .values(2)
                    .put(TrackDownloads._ID, track.getNumericId())
                    .put(TrackDownloads.REMOVED_AT, dateProvider.getCurrentTime())
                    .get());
        }
        return contentValues;
    }

    private List<ContentValues> buildContentValuesForDownloaded(List<DownloadRequest> downloadedTracks) {
        List<ContentValues> contentValues = new ArrayList<>(downloadedTracks.size());
        for (DownloadRequest request : downloadedTracks) {
            contentValues.add(ContentValuesBuilder
                    .values(4)
                    .put(_ID, request.track.getNumericId())
                    .put(UNAVAILABLE_AT, null)
                    .put(REMOVED_AT, null)
                    .put(DOWNLOADED_AT, dateProvider.getCurrentTime())
                    .get());
        }
        return contentValues;
    }

    private List<ContentValues> buildContentValuesForPendingDownload(List<DownloadRequest> pendingDownloads) {
        List<ContentValues> contentValues = new ArrayList<>(pendingDownloads.size());
        for (DownloadRequest request : pendingDownloads) {
            contentValues.add(ContentValuesBuilder
                    .values()
                    .put(_ID, request.track.getNumericId())
                    .put(REQUESTED_AT, dateProvider.getCurrentTime())
                    .put(REMOVED_AT, null)
                    .put(DOWNLOADED_AT, null)
                    .get());
        }
        return contentValues;
    }
}
