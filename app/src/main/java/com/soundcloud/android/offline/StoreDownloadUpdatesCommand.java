package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.UNAVAILABLE_AT;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
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
    protected StoreDownloadUpdatesCommand(PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        super(propeller);
        this.dateProvider = dateProvider;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final OfflineContentUpdates requests) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.bulkUpsert(TrackDownloads.TABLE, forRemoval(requests.tracksToRemove())));
                step(propeller.bulkUpsert(TrackDownloads.TABLE, forDownloaded(requests.tracksToRestore())));
                step(propeller.bulkUpsert(TrackDownloads.TABLE, forPendingDownload(requests.newTracksToDownload())));
                step(propeller.bulkUpsert(TrackDownloads.TABLE, forUnavailable(requests.unavailableTracks())));
            }
        });
    }

    private List<ContentValues> forUnavailable(Collection<Urn> creatorOptOut) {
        List<ContentValues> contentValues = new ArrayList<>(creatorOptOut.size());
        for (Urn track : creatorOptOut) {
            contentValues.add(ContentValuesBuilder.values(3)
                    .put(TrackDownloads.UNAVAILABLE_AT, dateProvider.getCurrentTime())
                    .put(TrackDownloads.REQUESTED_AT, null)
                    .put(TrackDownloads._ID, track.getNumericId())
                    .get());
        }
        return contentValues;
    }

    private List<ContentValues> forRemoval(List<Urn> removedTracks) {
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

    private List<ContentValues> forDownloaded(List<Urn> downloadedTracks) {
        List<ContentValues> contentValues = new ArrayList<>(downloadedTracks.size());
        for (Urn track : downloadedTracks) {
            contentValues.add(ContentValuesBuilder
                    .values(4)
                    .put(_ID, track.getNumericId())
                    .put(UNAVAILABLE_AT, null)
                    .put(REMOVED_AT, null)
                    .put(DOWNLOADED_AT, dateProvider.getCurrentTime())
                    .get());
        }
        return contentValues;
    }

    private List<ContentValues> forPendingDownload(List<Urn> pendingDownloads) {
        List<ContentValues> contentValues = new ArrayList<>(pendingDownloads.size());
        for (Urn track : pendingDownloads) {
            contentValues.add(ContentValuesBuilder
                    .values()
                    .put(_ID, track.getNumericId())
                    .put(REQUESTED_AT, dateProvider.getCurrentTime())
                    .put(REMOVED_AT, null)
                    .put(DOWNLOADED_AT, null)
                    .put(UNAVAILABLE_AT, null)
                    .get());
        }
        return contentValues;
    }
}
