package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.collection.DbModel.PlayHistory.FACTORY;
import static com.soundcloud.android.collection.playhistory.PlayHistoryModel.DeleteRowByIdAndTimestamp;
import static com.soundcloud.android.collection.playhistory.PlayHistoryModel.TABLE_NAME;
import static com.soundcloud.android.collection.playhistory.PlayHistoryModel.TIMESTAMP;
import static com.soundcloud.android.collection.playhistory.PlayHistoryModel.TRACK_ID;

import com.soundcloud.android.collection.CollectionDatabase;
import com.soundcloud.android.model.Urn;
import com.squareup.sqldelight.RowMapper;
import io.reactivex.Single;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.List;

public class PlayHistoryStorage {

    private static final RowMapper<Urn> URN_ROW_MAPPER = cursor -> Urn.forTrack(cursor.getLong(cursor.getColumnIndex(TRACK_ID)));

    private static final RowMapper<PlayHistoryRecord> PLAY_HISTORY_RECORD_ROW_MAPPER = cursor -> {
        final int timestampIndex = cursor.getColumnIndex(TIMESTAMP);
        final int trackIdIndex = cursor.getColumnIndex(TRACK_ID);
        return PlayHistoryRecord.create(
                cursor.getLong(timestampIndex),
                Urn.forTrack(cursor.getLong(trackIdIndex)),
                Urn.NOT_SET);
    };

    private final CollectionDatabase collectionDatabase;

    @Inject
    public PlayHistoryStorage(CollectionDatabase collectionDatabase) {
        this.collectionDatabase = collectionDatabase;
    }

    Single<List<Urn>> loadTrackUrns(int limit) {
        return collectionDatabase.executeAsyncQuery(FACTORY.selectUniqueTrackIdsWithLimit(limit), URN_ROW_MAPPER);
    }

    Single<List<Urn>> loadTrackUrnsForPlayback() {
        return collectionDatabase.executeAsyncQuery(FACTORY.selectUniqueTrackIds(), URN_ROW_MAPPER);
    }

    List<PlayHistoryRecord> loadUnSynced() {
        return collectionDatabase.executeQuery(FACTORY.selectTracksBySyncStatus(false), PLAY_HISTORY_RECORD_ROW_MAPPER);
    }

    @VisibleForTesting
    List<PlayHistoryRecord> loadAll() {
        return collectionDatabase.executeQuery(FACTORY.selectAll(), PLAY_HISTORY_RECORD_ROW_MAPPER);
    }

    List<PlayHistoryRecord> loadSynced() {
        return collectionDatabase.executeQuery(FACTORY.selectTracksBySyncStatus(true), PLAY_HISTORY_RECORD_ROW_MAPPER);
    }

    void insert(List<PlayHistoryRecord> records) {
        insertAll(records);
    }

    private void insertAll(List<PlayHistoryRecord> playHistoryRecords) {
        collectionDatabase.runInTransaction(() -> {
            PlayHistoryModel.InsertRow insertRow = new PlayHistoryModel.InsertRow(collectionDatabase.writableDatabase());
            for (PlayHistoryRecord playHistoryRecord : playHistoryRecords) {
                insertRow.bind(playHistoryRecord.trackUrn().getNumericId(), playHistoryRecord.timestamp(), true);
                collectionDatabase.insert(TABLE_NAME, insertRow.program);
            }
        });
    }

    public void upsertRow(PlayHistoryRecord playHistoryRecord) {
        PlayHistoryModel.UpsertRow upsertRow = new PlayHistoryModel.UpsertRow(collectionDatabase.writableDatabase());
        upsertRow.bind(playHistoryRecord.trackUrn().getNumericId(), playHistoryRecord.timestamp());
        collectionDatabase.insert(TABLE_NAME, upsertRow.program);
    }

    void removeAll(final List<PlayHistoryRecord> removeRecords) {
        collectionDatabase.runInTransaction(() -> {
            DeleteRowByIdAndTimestamp delete = new DeleteRowByIdAndTimestamp(collectionDatabase.writableDatabase());
            for (PlayHistoryRecord record : removeRecords) {
                delete.bind(record.trackUrn().getNumericId(), record.timestamp());
                collectionDatabase.updateOrDelete(TABLE_NAME, delete.program);
            }
        });
    }

    boolean hasPendingItemsToSync() {
        final List<Long> resultList = collectionDatabase.executeQuery(FACTORY.unsyncedTracksCount(), FACTORY.unsyncedTracksCountMapper());
        long unsyncedTracksCount = resultList.get(0);
        return unsyncedTracksCount > 0;
    }

    void trim(int limit) {
        PlayHistoryModel.Trim trim = new PlayHistoryModel.Trim(collectionDatabase.writableDatabase());
        trim.bind(limit);
        collectionDatabase.updateOrDelete(TABLE_NAME, trim.program);
    }

    public void clear() {
        collectionDatabase.clear(TABLE_NAME);
    }

}
