package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.tracks.TrackItemMapper.BASE_TRACK_FIELDS;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

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
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryStorage {

    private static final String TRACK_STATIONS_URN_PREFIX = "soundcloud:track-stations:";
    private static final String ARTIST_STATIONS_URN_PREFIX = "soundcloud:artist-stations:";

    private final PropellerDatabase database;
    private final PropellerRx rxDatabase;

    @Inject
    public PlayHistoryStorage(PropellerDatabase database) {
        this.database = database;
        this.rxDatabase = new PropellerRx(database);
    }

    public Observable<TrackItem> fetchTracks(int limit) {
        return rxDatabase.query(fetchTracksQuery(limit))
                         .map(new TrackItemMapper())
                         .map(TrackItem.fromPropertySet());
    }

    public Observable<RecentlyPlayedItem> fetchContexts(int limit) {
        return rxDatabase.query(fetchContextsQuery(limit))
                         .map(new RecentlyPlayedItemMapper());
    }

    Observable<Urn> fetchPlayHistoryForPlayback() {
        return rxDatabase.query(fetchForPlaybackQuery()).map(new Func1<CursorReader, Urn>() {
            @Override
            public Urn call(CursorReader cursorReader) {
                return Urn.forTrack(cursorReader.getLong(PlayHistory.TRACK_ID.name()));
            }
        });
    }

    public void clear() {
        database.delete(PlayHistory.TABLE);
    }

    private Query fetchTracksQuery(int limit) {
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

    private String fetchContextsQuery(int limit) {
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

    private Query fetchForPlaybackQuery() {
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
}
