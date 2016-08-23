package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.RecentlyPlayed;
import com.soundcloud.java.functions.Function;
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
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class RecentlyPlayedStorage {

    private static final String TRACK_STATIONS_URN_PREFIX = "soundcloud:track-stations:";
    private static final String ARTIST_STATIONS_URN_PREFIX = "soundcloud:artist-stations:";

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
        Collection<ContentValues> contentValues = buildSyncedContentValuesForContext(playHistoryRecords);
        return database.bulkUpsert(RecentlyPlayed.TABLE, contentValues);
    }

    TxnResult insertRecentlyPlayed(List<PlayHistoryRecord> addRecords) {
        return database.bulkUpsert(RecentlyPlayed.TABLE, buildSyncedContentValuesForContext(addRecords));
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

    Observable<RecentlyPlayedPlayableItem> loadContexts(int limit) {
        return rxDatabase.query(loadContextsQuery(limit))
                         .map(new RecentlyPlayedItemMapper());
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

    private List<PlayHistoryRecord> syncedRecentlyPlayed(boolean synced) {
        return database.query(loadSyncedRecentlyPlayedQuery(synced))
                       .toList(new ResultMapper<PlayHistoryRecord>() {
                           @Override
                           public PlayHistoryRecord map(CursorReader reader) {
                               int contextType = reader.getInt(RecentlyPlayed.CONTEXT_TYPE);
                               long contextId = reader.getLong(RecentlyPlayed.CONTEXT_ID);
                               Urn contextUrn = PlayHistoryRecord.contextUrnFor(contextType, contextId);
                               return PlayHistoryRecord.create(
                                       reader.getLong(RecentlyPlayed.TIMESTAMP),
                                       Urn.NOT_SET,
                                       contextUrn);
                           }
                       });
    }

    private Query loadSyncedRecentlyPlayedQuery(boolean synced) {
        return Query.from(RecentlyPlayed.TABLE)
                    .select(RecentlyPlayed.TIMESTAMP, RecentlyPlayed.CONTEXT_TYPE, RecentlyPlayed.CONTEXT_ID)
                    .whereEq(RecentlyPlayed.SYNCED, synced);
    }

    private Collection<ContentValues> buildSyncedContentValuesForContext(Collection<PlayHistoryRecord> records) {
        return transform(records, new Function<PlayHistoryRecord, ContentValues>() {
            public ContentValues apply(PlayHistoryRecord input) {
                ContentValues contentValues = buildSyncedContentValues();
                contentValues.put(RecentlyPlayed.TIMESTAMP.name(), input.timestamp());
                contentValues.put(RecentlyPlayed.CONTEXT_TYPE.name(), input.getContextType());
                contentValues.put(RecentlyPlayed.CONTEXT_ID.name(), input.contextUrn().getNumericId());
                return contentValues;
            }
        });
    }

    private Where buildMatchFilter(PlayHistoryRecord record) {
        return filter()
                .whereEq(RecentlyPlayed.TIMESTAMP, record.timestamp())
                .whereEq(RecentlyPlayed.CONTEXT_TYPE, record.getContextType())
                .whereEq(RecentlyPlayed.CONTEXT_ID, record.contextUrn().getNumericId());
    }

    private ContentValues buildSyncedContentValues() {
        ContentValues asSynced = new ContentValues();
        asSynced.put(RecentlyPlayed.SYNCED.name(), true);
        return asSynced;
    }

    private static class RecentlyPlayedItemMapper extends RxResultMapper<RecentlyPlayedPlayableItem> {
        static final String COLUMN_ARTWORK_URL = "artwork_url";
        static final String COLUMN_COLLECTION_ALBUM = "collection_album";
        static final String COLUMN_COLLECTION_COUNT = "collection_count";
        static final String COLUMN_TITLE = "recently_played_title";

        @Override
        public RecentlyPlayedPlayableItem map(CursorReader reader) {
            Urn urn = PlayHistoryRecord.contextUrnFor(
                    reader.getInt(RecentlyPlayed.CONTEXT_TYPE.name()),
                    reader.getLong(RecentlyPlayed.CONTEXT_ID.name()));

            return RecentlyPlayedPlayableItem.create(
                    urn,
                    Optional.fromNullable(reader.getString(COLUMN_ARTWORK_URL)),
                    reader.getString(COLUMN_TITLE),
                    reader.getInt(COLUMN_COLLECTION_COUNT),
                    reader.getBoolean(COLUMN_COLLECTION_ALBUM));
        }
    }

}
