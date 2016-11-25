package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMapper;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.schema.BulkInsertValues;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PlayHistoryStorage {

    private final PropellerDatabase database;
    private final PropellerRx rxDatabase;

    @Inject
    public PlayHistoryStorage(PropellerDatabase database) {
        this.database = database;
        this.rxDatabase = new PropellerRx(database);
    }

    Observable<TrackItem> loadTracks(int limit) {
        return rxDatabase.query(loadTracksQuery(limit))
                         .map(new TrackItemMapper())
                         .map(TrackItem.fromPropertySet());
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

    Observable<Urn> loadPlayHistoryForPlayback() {
        return rxDatabase.query(loadForPlaybackQuery()).map(new Func1<CursorReader, Urn>() {
            @Override
            public Urn call(CursorReader cursorReader) {
                return Urn.forTrack(cursorReader.getLong(PlayHistory.TRACK_ID.name()));
            }
        });
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
                    .select(Tables.TrackView.TABLE.name() + ".*",
                            field("max("+PlayHistory.TIMESTAMP.name()+")").as("max_timestamp"))
                    .innerJoin(Tables.TrackView.TABLE, filter()
                            .whereEq(Tables.TrackView.ID, PlayHistory.TRACK_ID))
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
                       .toList(new ResultMapper<PlayHistoryRecord>() {
                           @Override
                           public PlayHistoryRecord map(CursorReader reader) {
                               return PlayHistoryRecord.create(
                                       reader.getLong(PlayHistory.TIMESTAMP),
                                       Urn.forTrack(reader.getLong(PlayHistory.TRACK_ID)),
                                       Urn.NOT_SET);
                           }
                       });
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
