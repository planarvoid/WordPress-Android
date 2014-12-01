package com.soundcloud.android.offline;

import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.*;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TrackDownloadsStorage {

    private final PropellerDatabase database;

    @Inject
    TrackDownloadsStorage(PropellerDatabase database) {
        this.database = database;
    }

    List<DownloadRequest> getPendingDownloads() {
        final Query q = Query.from(TrackDownloads.name(), Sounds.name())
                .joinOn(Sounds + "." + TableColumns.Sounds._ID, TrackDownloads + "." + _ID)
                .select(TrackDownloads + "." + _ID, TableColumns.SoundView.STREAM_URL)
                .whereEq(DOWNLOADED_AT, 0)
                .order(REQUESTED_AT, Query.ORDER_ASC);
        return database.query(q).toList(new DownloadRequestMapper());
    }

    TxnResult storeRequestedDownloads(List<Urn> tracks) {
        final List<ContentValues> contentValuesList = buildContentValuesFromUrns(tracks);
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ContentValues contentValues : contentValuesList) {
                    step(database.insert(TrackDownloads, contentValues, SQLiteDatabase.CONFLICT_IGNORE));
                }
            }
        });
    }

    ChangeResult updateDownload(DownloadResult downloads) {
        return database.upsert(TrackDownloads, buildContentValues(downloads));
    }

    private List<ContentValues> buildContentValuesFromUrns(final Collection<Urn> downloadRequests) {
        final long requestedTime = System.currentTimeMillis();
        final List<ContentValues> newItems = new ArrayList<>(downloadRequests.size());

        for (Urn urn : downloadRequests) {
            newItems.add(ContentValuesBuilder.values()
                    .put(_ID, urn.getNumericId())
                    .put(REQUESTED_AT, requestedTime)
                    .get());
        }
        return newItems;
    }

    private ContentValues buildContentValues(DownloadResult downloadResult) {
        return ContentValuesBuilder.values(2)
                .put(_ID, downloadResult.getUrn().getNumericId())
                .put(DOWNLOADED_AT, downloadResult.getDownloadedAt())
                .get();
        }

    private static class DownloadRequestMapper implements ResultMapper<DownloadRequest> {
        @Override
        public DownloadRequest map(CursorReader reader) {
            return new DownloadRequest(
                    Urn.forTrack(reader.getLong(_ID)),
                    reader.getString(TableColumns.SoundView.STREAM_URL));
        }
    }

}
