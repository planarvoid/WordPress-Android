package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.tracks.TrackItemMapper.BASE_TRACK_FIELDS;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMapper;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
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
        Collection<ContentValues> contentValues = buildSyncedContentValuesForTrack(playHistoryRecords);
        database.bulkUpsert(PlayHistory.TABLE, contentValues);
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
        return database.bulkUpsert(PlayHistory.TABLE, buildSyncedContentValuesForTrack(addRecords));
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
        List<Object> fields = new ArrayList<>(BASE_TRACK_FIELDS.size() + 2);
        fields.addAll(BASE_TRACK_FIELDS);
        // These fields are required by TrackItemMapper but not needed by the ItemRenderer
        // field("0").as(...) sets the field to false
        fields.add(field("0").as(SoundView.USER_LIKE));
        fields.add(field("0").as(SoundView.USER_REPOST));
        fields.add(field("max("+PlayHistory.TIMESTAMP.name()+")").as("max_timestamp"));

        return Query.from(PlayHistory.TABLE)
                    .select(fields.toArray())
                    .innerJoin(Table.SoundView, filter()
                            .whereEq(Sounds._ID, PlayHistory.TRACK_ID)
                            .whereEq(Sounds._TYPE, Sounds.TYPE_TRACK))
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

    private Collection<ContentValues> buildSyncedContentValuesForTrack(Collection<PlayHistoryRecord> records) {
        return transform(records, new Function<PlayHistoryRecord, ContentValues>() {
            public ContentValues apply(PlayHistoryRecord input) {
                ContentValues contentValues = buildSyncedContentValues();
                contentValues.put(PlayHistory.TIMESTAMP.name(), input.timestamp());
                contentValues.put(PlayHistory.TRACK_ID.name(), input.trackUrn().getNumericId());
                return contentValues;
            }
        });
    }

    private Where buildMatchFilter(PlayHistoryRecord record) {
        return filter()
                .whereEq(PlayHistory.TIMESTAMP, record.timestamp())
                .whereEq(PlayHistory.TRACK_ID, record.trackUrn().getNumericId());
    }

    private ContentValues buildSyncedContentValues() {
        ContentValues asSynced = new ContentValues();
        asSynced.put(PlayHistory.SYNCED.name(), true);
        return asSynced;
    }
}
