package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRxV2;
import com.soundcloud.propeller.rx.RxResultMapperV2;
import com.soundcloud.propeller.schema.BulkInsertValues;
import io.reactivex.Observable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PlayHistoryStorage {

    private static final RxResultMapperV2<Urn> URN_MAPPER = new RxResultMapperV2<Urn>() {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(PlayHistory.TRACK_ID.name()));
        }
    };

    private final PropellerDatabase database;
    private final PropellerRxV2 rxDatabase;

    @Inject
    public PlayHistoryStorage(PropellerDatabase database) {
        this.database = database;
        this.rxDatabase = new PropellerRxV2(database);
    }

    Single<List<Urn>> loadTracks(int limit) {
        return rxDatabase.queryResult(loadTracksQuery(limit))
                         .map(result -> result.toList(URN_MAPPER))
                         .singleOrError();
    }

    List<PlayHistoryRecord> loadUnSyncedPlayHistory() {
        return syncedPlayHistory(false);
    }

    List<PlayHistoryRecord> loadSyncedPlayHistory() {
        return syncedPlayHistory(true);
    }

    void setSynced(List<PlayHistoryRecord> playHistoryRecords) {
        database.bulkInsert(PlayHistory.TABLE, buildBulkValues(playHistoryRecords));
    }

    TxnResult removePlayHistory(final List<PlayHistoryRecord> removeRecords) {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (PlayHistoryRecord removeRecord : removeRecords) {
                    step(database.delete(PlayHistory.TABLE, buildMatchFilter(removeRecord)));
                    if (!success()) {
                        break;
                    }
                }
            }
        });
    }

    TxnResult insertPlayHistory(List<PlayHistoryRecord> addRecords) {
        return database.bulkInsert(PlayHistory.TABLE, buildBulkValues(addRecords));
    }

    Single<List<Urn>> loadPlayHistoryForPlayback() {
        return rxDatabase.queryResult(loadForPlaybackQuery())
                         .flatMap(Observable::fromIterable)
                         .map(URN_MAPPER)
                         .filter(Urn::isTrack)
                         .toList();
    }

    boolean hasPendingTracksToSync() {
        Query query = Query.from(PlayHistory.TABLE)
                           .select(count(PlayHistory.TRACK_ID))
                           .whereEq(PlayHistory.SYNCED, false);

        return database.query(query).first(scalar(Integer.class)) > 0;
    }

    public void clear() {
        database.delete(PlayHistory.TABLE);
    }

    private Query loadTracksQuery(int limit) {
        return Query.from(PlayHistory.TABLE)
                    .select(PlayHistory.TRACK_ID.name(),
                            field("max(" + PlayHistory.TIMESTAMP.name() + ")").as("max_timestamp"))
                    .groupBy(PlayHistory.TRACK_ID)
                    .order("max_timestamp", Query.Order.DESC)
                    .limit(limit);
    }

    private Query loadForPlaybackQuery() {
        return Query.from(PlayHistory.TABLE.name())
                    .select("DISTINCT " + PlayHistory.TRACK_ID.name())
                    .order(PlayHistory.TIMESTAMP, Query.Order.DESC);
    }

    private List<PlayHistoryRecord> syncedPlayHistory(boolean synced) {
        return database.query(loadSyncedTracksQuery(synced))
                       .toList(reader -> PlayHistoryRecord.create(
                               reader.getLong(PlayHistory.TIMESTAMP),
                               Urn.forTrack(reader.getLong(PlayHistory.TRACK_ID)),
                               Urn.NOT_SET));
    }

    private Query loadSyncedTracksQuery(boolean synced) {
        return Query.from(PlayHistory.TABLE)
                    .select(PlayHistory.TIMESTAMP, PlayHistory.TRACK_ID)
                    .whereEq(PlayHistory.SYNCED, synced);
    }

    private BulkInsertValues buildBulkValues(Collection<PlayHistoryRecord> records) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(Arrays.asList(
                PlayHistory.SYNCED, PlayHistory.TIMESTAMP, PlayHistory.TRACK_ID
        ));

        for (PlayHistoryRecord record : records) {
            builder.addRow(Arrays.asList(true, record.timestamp(), record.trackUrn().getNumericId()));
        }
        return builder.build();
    }

    private Where buildMatchFilter(PlayHistoryRecord record) {
        return filter()
                .whereEq(PlayHistory.TIMESTAMP, record.timestamp())
                .whereEq(PlayHistory.TRACK_ID, record.trackUrn().getNumericId());
    }
}
