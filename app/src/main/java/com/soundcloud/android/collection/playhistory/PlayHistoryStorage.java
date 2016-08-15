package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.tracks.TrackItemMapper.BASE_TRACK_FIELDS;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.storage.Tables.RecentlyPlayed;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMapper;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayHistoryStorage {

    private static final String TRACK_STATIONS_URN_PREFIX = "soundcloud:track-stations:";
    private static final String ARTIST_STATIONS_URN_PREFIX = "soundcloud:artist-stations:";

    private static final Function<PlayHistoryRecord, Long> TO_TRACK_IDS = new Function<PlayHistoryRecord, Long>() {
        public Long apply(PlayHistoryRecord input) {
            return input.trackUrn().getNumericId();
        }
    };
    private static final int MAX_WHERE_IN = 500;

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
        ContentValues contentValues = buildSyncedContentValues();

        for (List<PlayHistoryRecord> historyRecords : Lists.partition(playHistoryRecords, MAX_WHERE_IN)) {
            Where conditions = filter().whereIn(PlayHistory.TRACK_ID, transform(historyRecords, TO_TRACK_IDS));
            database.update(PlayHistory.TABLE, contentValues, conditions);
        }
    }

    void removePlayHistory(List<PlayHistoryRecord> removeRecords) {
        for (List<PlayHistoryRecord> historyRecords : Lists.partition(removeRecords, MAX_WHERE_IN)) {
            Where conditions = filter().whereIn(PlayHistory.TRACK_ID, transform(historyRecords, TO_TRACK_IDS));
            database.delete(PlayHistory.TABLE, conditions);
        }
    }

    TxnResult insertPlayHistory(List<PlayHistoryRecord> addRecords) {
        return database.bulkInsert(PlayHistory.TABLE, buildSyncedContentValuesForTrack(addRecords));
    }

    Observable<RecentlyPlayedItem> loadContexts(int limit) {
        return rxDatabase.query(loadContextsQuery(limit))
                         .map(new RecentlyPlayedItemMapper());
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

        return Query.from(PlayHistory.TABLE)
                    .select(fields.toArray())
                    .innerJoin(Table.SoundView, filter()
                            .whereEq(Sounds._ID, PlayHistory.TRACK_ID)
                            .whereEq(Sounds._TYPE, Sounds.TYPE_TRACK))
                    .groupBy(PlayHistory.TRACK_ID)
                    .order(PlayHistory.TIMESTAMP, Query.Order.DESC)
                    .limit(limit);
    }

    private String loadContextsQuery(int limit) {
        return "SELECT " +
                "    rp.context_type as " + RecentlyPlayed.CONTEXT_TYPE.name() + "," +
                "    rp.context_id as " + RecentlyPlayed.CONTEXT_ID.name() + "," +
                "    coalesce(playlists.title, artist_stations.title, track_stations.title, users.username) as " + RecentlyPlayedItemMapper.COLUMN_TITLE + "," +
                "    coalesce(playlists.artwork_url, artist_stations.artwork_url_template, artist_stations.artwork_url_template, users.avatar_url, ptv.artwork_url) as " + RecentlyPlayedItemMapper.COLUMN_ARTWORK_URL + "," +
                "    coalesce(playlists.track_count, 0) as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_COUNT + "," +
                "    coalesce(playlists.is_album, 0) as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_ALBUM +
                "  FROM " + RecentlyPlayed.TABLE.name() + " as rp" +
                "  LEFT JOIN " + Table.SoundView.name() + " as playlists ON rp.context_type = " + PlayHistoryRecord.CONTEXT_PLAYLIST + " AND playlists._type = " + Sounds.TYPE_PLAYLIST + " AND playlists._id = rp.context_id" +
                "  LEFT JOIN " + Tables.Stations.TABLE.name() + " as track_stations ON rp.context_type = " + PlayHistoryRecord.CONTEXT_TRACK_STATION + " AND track_stations.station_urn = '" + TRACK_STATIONS_URN_PREFIX + "' || rp.context_id" +
                "  LEFT JOIN " + Tables.Stations.TABLE.name() + " as artist_stations ON rp.context_type = " + PlayHistoryRecord.CONTEXT_ARTIST_STATION + " AND artist_stations.station_urn = '" + ARTIST_STATIONS_URN_PREFIX + "' || rp.context_id" +
                "  LEFT JOIN " + Table.Users.name() + " as users ON rp.context_type = " + PlayHistoryRecord.CONTEXT_ARTIST + " AND users._id = rp.context_id" +
                "  LEFT JOIN " + Table.PlaylistTracksView.name() + " as ptv ON ptv.playlist_id = playlists._ID AND playlist_position = 0" +
                "  WHERE rp.context_type != " + PlayHistoryRecord.CONTEXT_OTHER + " AND " + RecentlyPlayedItemMapper.COLUMN_TITLE + " IS NOT NULL" +
                "  GROUP BY rp.context_type, rp.context_id" +
                "  ORDER BY rp.timestamp DESC" +
                "  LIMIT " + limit;
    }

    private Query loadForPlaybackQuery() {
        return Query.from(PlayHistory.TABLE.name())
                    .select("DISTINCT " + PlayHistory.TRACK_ID.name())
                    .order(PlayHistory.TIMESTAMP, Query.Order.DESC);
    }

    private class RecentlyPlayedItemMapper extends RxResultMapper<RecentlyPlayedItem> {
        static final String COLUMN_ARTWORK_URL = "artwork_url";
        static final String COLUMN_COLLECTION_ALBUM = "collection_album";
        static final String COLUMN_COLLECTION_COUNT = "collection_count";
        static final String COLUMN_TITLE = "recently_played_title";

        @Override
        public RecentlyPlayedItem map(CursorReader reader) {
            Urn urn = PlayHistoryRecord.contextUrnFor(
                    reader.getInt(RecentlyPlayed.CONTEXT_TYPE.name()),
                    reader.getLong(RecentlyPlayed.CONTEXT_ID.name()));

            return RecentlyPlayedItem.create(
                    urn,
                    Optional.fromNullable(reader.getString(COLUMN_ARTWORK_URL)),
                    reader.getString(COLUMN_TITLE),
                    reader.getInt(COLUMN_COLLECTION_COUNT),
                    reader.getBoolean(COLUMN_COLLECTION_ALBUM));
        }
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

    private ContentValues buildSyncedContentValues() {
        ContentValues asSynced = new ContentValues();
        asSynced.put(PlayHistory.SYNCED.name(), true);
        return asSynced;
    }
}
