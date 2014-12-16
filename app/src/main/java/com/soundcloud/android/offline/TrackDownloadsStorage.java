package com.soundcloud.android.offline;

import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads._ID;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class TrackDownloadsStorage {

    private final PropellerDatabase database;
    private final DatabaseScheduler scheduler;

    @Inject
    TrackDownloadsStorage(PropellerDatabase database, DatabaseScheduler scheduler) {
        this.database = database;
        this.scheduler = scheduler;
    }
    
    Observable<TxnResult> filterAndStoreNewDownloadRequests(List<Urn> tracks) {
        // It looks like the SQL insert strategy with CONFLICT_IGNORE does not work as expected
        // returns -1 when the row with given ID already exists. So we do filtering out ourselves :(
        final List<Urn> existingDownloads = getPendingDownloadUrns();
        return storeRequestedDownloads(filterDuplicates(tracks, existingDownloads));
    }

    Observable<List<DownloadRequest>> getPendingDownloads() {
        final Query q = Query.from(TrackDownloads.name(), Sounds.name())
                .joinOn(Sounds + "." + TableColumns.Sounds._ID, TrackDownloads + "." + _ID)
                .select(TrackDownloads + "." + _ID, TableColumns.SoundView.STREAM_URL)
                .whereEq(DOWNLOADED_AT, 0);
        return scheduler.scheduleQuery(q).map(new DownloadRequestMapper()).toList();
    }

    private Iterable<Urn> filterDuplicates(List<Urn> tracks, final List<Urn> existingDownloads) {
        return Iterables.filter(tracks, new Predicate<Urn>() {
            @Override
            public boolean apply(@Nullable Urn urn) {
                return !existingDownloads.contains(urn);
            }
        });
    }

    private List<Urn> getPendingDownloadUrns() {
        final Query query = Query.from(TrackDownloads.name()).select(_ID);
        return database.query(query).toList(new UrnMapper());
    }

    private Observable<TxnResult> storeRequestedDownloads(Iterable<Urn> tracks) {
        final List<ContentValues> contentValuesList = buildContentValuesFromUrns(tracks);
        return scheduler.scheduleTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ContentValues contentValues : contentValuesList) {
                    step(database.insert(TrackDownloads, contentValues));
                }
            }
        });
    }

    ChangeResult updateDownload(DownloadResult downloads) {
        return database.upsert(TrackDownloads, buildContentValues(downloads));
    }

    private List<ContentValues> buildContentValuesFromUrns(final Iterable<Urn> downloadRequests) {
        final long requestedTime = System.currentTimeMillis();
        final List<ContentValues> newItems = new ArrayList<>();

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

    private static class DownloadRequestMapper extends RxResultMapper<DownloadRequest> {
        @Override
        public DownloadRequest map(CursorReader reader) {
            return new DownloadRequest(
                    Urn.forTrack(reader.getLong(_ID)),
                    reader.getString(TableColumns.SoundView.STREAM_URL));
        }
    }

    private static class UrnMapper implements ResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forTrack(reader.getLong(_ID));
        }
    }
}
