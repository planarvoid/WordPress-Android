package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.UNAVAILABLE_AT;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

class StoreDownloadUpdatesCommand extends DefaultWriteStorageCommand<OfflineContentRequests, WriteResult> {

    @Inject
    protected StoreDownloadUpdatesCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final OfflineContentRequests requests) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (Urn urn : requests.newRemovedTracks) {
                    step(propeller.upsert(TrackDownloads, buildContentValuesForRemoval(urn)));
                }

                for (DownloadRequest downloadRequest : requests.newRestoredRequests) {
                    step(propeller.upsert(TrackDownloads, buildContentValuesForDownloaded(downloadRequest.track)));
                }

                for (DownloadRequest downloadRequest : requests.newDownloadRequests) {
                    step(propeller.upsert(TrackDownloads, buildContentValuesForPendingDownload(downloadRequest.track)));
                }
            }
        });
    }

    private ContentValues buildContentValuesForRemoval(Urn urn) {
        return ContentValuesBuilder
                .values()
                .put(_ID, urn.getNumericId())
                .put(TableColumns.TrackDownloads.REMOVED_AT, System.currentTimeMillis())
                .get();
    }

    private ContentValues buildContentValuesForDownloaded(Urn urn) {
        return ContentValuesBuilder
                .values()
                .put(_ID, urn.getNumericId())
                .put(UNAVAILABLE_AT, null)
                .put(REMOVED_AT, null)
                .put(DOWNLOADED_AT, System.currentTimeMillis())
                .get();
    }

    private ContentValues buildContentValuesForPendingDownload(Urn urn) {
        return ContentValuesBuilder
                .values()
                .put(_ID, urn.getNumericId())
                .put(TableColumns.TrackDownloads.REQUESTED_AT, System.currentTimeMillis())
                .put(REMOVED_AT, null)
                .put(TableColumns.TrackDownloads.DOWNLOADED_AT, null)
                .get();
    }
}
