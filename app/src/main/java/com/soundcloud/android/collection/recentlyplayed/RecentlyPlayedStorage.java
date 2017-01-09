package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.RecentlyPlayed;
import com.soundcloud.android.storage.Tables.Users;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.BulkInsertValues;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecentlyPlayedStorage {

    private static final String TRACK_STATIONS_URN_PREFIX = "soundcloud:track-stations:";
    private static final String ARTIST_STATIONS_URN_PREFIX = "soundcloud:artist-stations:";
    public static final Func2<RecentlyPlayedPlayableItem, RecentlyPlayedPlayableItem, Integer> SORT_BY_TIMESTAMP = (a, b) -> {
        final long l = b.getTimestamp() - a.getTimestamp();
        return l > 0 ? 1 : l < 0 ? -1 : 0;
    };

    private final PropellerDatabase database;
    private final PropellerRx rxDatabase;

    @Inject
    public RecentlyPlayedStorage(PropellerDatabase database) {
        this.database = database;
        this.rxDatabase = new PropellerRx(database);
    }

    List<PlayHistoryRecord> loadUnSyncedRecentlyPlayed() {
        return syncedRecentlyPlayed(false);
    }

    List<PlayHistoryRecord> loadSyncedRecentlyPlayed() {
        return syncedRecentlyPlayed(true);
    }

    WriteResult setSynced(List<PlayHistoryRecord> playHistoryRecords) {
        return database.bulkInsert(RecentlyPlayed.TABLE, buildBulkValues(playHistoryRecords));
    }

    TxnResult insertRecentlyPlayed(List<PlayHistoryRecord> addRecords) {
        return database.bulkInsert(RecentlyPlayed.TABLE, buildBulkValues(addRecords));
    }

    private Map<String, Class> getColumns() {
        final HashMap<String, Class> columns = new HashMap<>(4);
        columns.put(RecentlyPlayed.TIMESTAMP.name(), Long.class);
        columns.put(RecentlyPlayed.CONTEXT_ID.name(), Long.class);
        columns.put(RecentlyPlayed.CONTEXT_TYPE.name(), Integer.class);
        columns.put(RecentlyPlayed.SYNCED.name(), Boolean.class);
        return columns;
    }

    TxnResult removeRecentlyPlayed(final List<PlayHistoryRecord> removeRecords) {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (PlayHistoryRecord removeRecord : removeRecords) {
                    step(database.delete(RecentlyPlayed.TABLE, buildMatchFilter(removeRecord)));
                    if (!success()) {
                        break;
                    }
                }
            }
        });
    }

    Observable<List<RecentlyPlayedPlayableItem>> loadContexts(final int limit) {
        final Observable<CursorReader> playlists = rxDatabase.query(playlistsQuery());
        final Observable<CursorReader> users = rxDatabase.query(usersQuery());
        final Observable<CursorReader> stations = rxDatabase.query(stationsQuery());

        return Observable.concat(playlists, users, stations)
                         .map(new RecentlyPlayedItemMapper())
                         .toSortedList(SORT_BY_TIMESTAMP)
                         .map(withLimit(limit));
    }

    private Func1<List<RecentlyPlayedPlayableItem>, List<RecentlyPlayedPlayableItem>> withLimit(final int limit) {
        return items -> items.subList(0, Math.min(limit, items.size()));
    }

    boolean hasPendingContextsToSync() {
        Query query = Query.from(RecentlyPlayed.TABLE)
                           .select(count(RecentlyPlayed.CONTEXT_ID))
                           .whereEq(RecentlyPlayed.SYNCED, false);

        return database.query(query).first(scalar(Integer.class)) > 0;
    }

    public void clear() {
        database.delete(RecentlyPlayed.TABLE);
    }

    private String playlistsQuery() {
        return "SELECT " +
                "    rp.context_type as " + RecentlyPlayed.CONTEXT_TYPE.name() + "," +
                "    rp.context_id as " + RecentlyPlayed.CONTEXT_ID.name() + "," +
                "    pv.pv_title as " + RecentlyPlayedItemMapper.COLUMN_TITLE + "," +
                "    pv.pv_artwork_url as " + RecentlyPlayedItemMapper.COLUMN_ARTWORK_URL + "," +
                "    pv.pv_track_count as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_COUNT + "," +
                "    pv.pv_is_album as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_ALBUM + "," +
                "    pv.pv_is_marked_for_offline as " + RecentlyPlayedItemMapper.COLUMN_MARKED_FOR_OFFLINE + "," +
                "    pv.pv_has_pending_download_request as " + RecentlyPlayedItemMapper.COLUMN_HAS_PENDING_DOWNLOAD_REQUEST + "," +
                "    pv.pv_has_downloaded_tracks as " + RecentlyPlayedItemMapper.COLUMN_HAS_DOWNLOADED_TRACKS + "," +
                "    pv.pv_has_unavailable_tracks as " + RecentlyPlayedItemMapper.COLUMN_HAS_UNAVAILABLE_TRACKS + "," +
                "    max(" + RecentlyPlayed.TIMESTAMP.name() + ") as " + RecentlyPlayedItemMapper.COLUMN_MAX_TIMESTAMP +
                "  FROM " + RecentlyPlayed.TABLE.name() + " as rp" +
                "  INNER JOIN " + Tables.PlaylistView.TABLE.name() + " as pv ON pv.pv_id = rp.context_id" +
                "  WHERE rp.context_type = " + PlayHistoryRecord.CONTEXT_PLAYLIST +
                "  GROUP BY rp.context_type, rp.context_id";
    }

    private String stationsQuery() {
        return "SELECT " +
                "    rp.context_type as " + RecentlyPlayed.CONTEXT_TYPE.name() + "," +
                "    rp.context_id as " + RecentlyPlayed.CONTEXT_ID.name() + "," +
                "    st.title " + RecentlyPlayedItemMapper.COLUMN_TITLE + "," +
                "    st.artwork_url_template as " + RecentlyPlayedItemMapper.COLUMN_ARTWORK_URL + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_COUNT + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_ALBUM + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_MARKED_FOR_OFFLINE + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_PENDING_DOWNLOAD_REQUEST + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_DOWNLOADED_TRACKS + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_UNAVAILABLE_TRACKS + "," +
                "    max(" + RecentlyPlayed.TIMESTAMP.name() + ") as " + RecentlyPlayedItemMapper.COLUMN_MAX_TIMESTAMP +
                "  FROM " + RecentlyPlayed.TABLE.name() + " as rp" +
                "  INNER JOIN " + Tables.Stations.TABLE.name() + " as st ON (rp.context_type = " + PlayHistoryRecord.CONTEXT_TRACK_STATION + " AND station_urn = '" + TRACK_STATIONS_URN_PREFIX + "' || rp.context_id) OR (rp.context_type = " + PlayHistoryRecord.CONTEXT_ARTIST_STATION + " AND station_urn = '" + ARTIST_STATIONS_URN_PREFIX + "' || rp.context_id)" +
                "  GROUP BY rp.context_type, rp.context_id";
    }

    private String usersQuery() {
        return "SELECT " +
                "    rp.context_type as " + RecentlyPlayed.CONTEXT_TYPE.name() + "," +
                "    rp.context_id as " + RecentlyPlayed.CONTEXT_ID.name() + "," +
                "    us.username " + RecentlyPlayedItemMapper.COLUMN_TITLE + "," +
                "    us.avatar_url as " + RecentlyPlayedItemMapper.COLUMN_ARTWORK_URL + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_COUNT + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_COLLECTION_ALBUM + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_MARKED_FOR_OFFLINE + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_PENDING_DOWNLOAD_REQUEST + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_DOWNLOADED_TRACKS + "," +
                "    0 as " + RecentlyPlayedItemMapper.COLUMN_HAS_UNAVAILABLE_TRACKS + "," +
                "    max(" + RecentlyPlayed.TIMESTAMP.name() + ") as " + RecentlyPlayedItemMapper.COLUMN_MAX_TIMESTAMP +
                "  FROM " + RecentlyPlayed.TABLE.name() + " as rp" +
                "  INNER JOIN " + Users.TABLE.name() + " as us ON _id = rp.context_id" +
                "  WHERE rp.context_type = " + PlayHistoryRecord.CONTEXT_ARTIST +
                "  GROUP BY rp.context_type, rp.context_id";
    }

    private List<PlayHistoryRecord> syncedRecentlyPlayed(boolean synced) {
        return database.query(loadSyncedRecentlyPlayedQuery(synced))
                       .toList(reader -> {
                           int contextType = reader.getInt(RecentlyPlayed.CONTEXT_TYPE);
                           long contextId = reader.getLong(RecentlyPlayed.CONTEXT_ID);
                           Urn contextUrn = PlayHistoryRecord.contextUrnFor(contextType, contextId);
                           return PlayHistoryRecord.create(
                                   reader.getLong(RecentlyPlayed.TIMESTAMP),
                                   Urn.NOT_SET,
                                   contextUrn);
                       });
    }

    private Query loadSyncedRecentlyPlayedQuery(boolean synced) {
        return Query.from(RecentlyPlayed.TABLE)
                    .select(RecentlyPlayed.TIMESTAMP, RecentlyPlayed.CONTEXT_TYPE, RecentlyPlayed.CONTEXT_ID)
                    .whereEq(RecentlyPlayed.SYNCED, synced);
    }

    private BulkInsertValues buildBulkValues(Collection<PlayHistoryRecord> records) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                Arrays.asList(
                        RecentlyPlayed.TIMESTAMP,
                        RecentlyPlayed.CONTEXT_ID,
                        RecentlyPlayed.CONTEXT_TYPE,
                        RecentlyPlayed.SYNCED
                )
        );

        for (PlayHistoryRecord record : records) {
            builder.addRow(Arrays.asList(
                    record.timestamp(), record.contextUrn().getNumericId(), record.getContextType(), true));
        }
        return builder.build();
    }

    private Where buildMatchFilter(PlayHistoryRecord record) {
        return filter()
                .whereEq(RecentlyPlayed.TIMESTAMP, record.timestamp())
                .whereEq(RecentlyPlayed.CONTEXT_TYPE, record.getContextType())
                .whereEq(RecentlyPlayed.CONTEXT_ID, record.contextUrn().getNumericId());
    }

    private static class RecentlyPlayedItemMapper extends RxResultMapper<RecentlyPlayedPlayableItem> {
        static final String COLUMN_ARTWORK_URL = "artwork_url";
        static final String COLUMN_COLLECTION_ALBUM = "collection_album";
        static final String COLUMN_COLLECTION_COUNT = "collection_count";
        static final String COLUMN_TITLE = "recently_played_title";
        static final String COLUMN_MARKED_FOR_OFFLINE = "marked_for_offline";
        static final String COLUMN_HAS_PENDING_DOWNLOAD_REQUEST = "has_pending_tracks";
        static final String COLUMN_HAS_DOWNLOADED_TRACKS = "has_downloaded_tracks";
        static final String COLUMN_HAS_UNAVAILABLE_TRACKS = "has_unavailable_tracks";
        static final String COLUMN_MAX_TIMESTAMP = "max_timestamp";

        @Override
        public RecentlyPlayedPlayableItem map(CursorReader reader) {
            Urn urn = PlayHistoryRecord.contextUrnFor(
                    reader.getInt(RecentlyPlayed.CONTEXT_TYPE.name()),
                    reader.getLong(RecentlyPlayed.CONTEXT_ID.name()));

            return new RecentlyPlayedPlayableItem(
                    urn,
                    Optional.fromNullable(reader.getString(COLUMN_ARTWORK_URL)),
                    reader.getString(COLUMN_TITLE),
                    reader.getInt(COLUMN_COLLECTION_COUNT),
                    reader.getBoolean(COLUMN_COLLECTION_ALBUM),
                    getOfflineState(reader),
                    reader.getLong(COLUMN_MAX_TIMESTAMP));
        }

        private Optional<OfflineState> getOfflineState(CursorReader cursorReader) {
            final boolean isMarkedForOffline = cursorReader.getBoolean(COLUMN_MARKED_FOR_OFFLINE);

            if (isMarkedForOffline) {
                return Optional.of(OfflineState.getOfflineState(
                        cursorReader.getBoolean(COLUMN_HAS_PENDING_DOWNLOAD_REQUEST),
                        cursorReader.getBoolean(COLUMN_HAS_DOWNLOADED_TRACKS),
                        cursorReader.getBoolean(COLUMN_HAS_UNAVAILABLE_TRACKS)));
            } else {
                return Optional.absent();
            }
        }
    }

}
