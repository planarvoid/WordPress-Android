package com.soundcloud.android.storage;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.playlists.PlaylistQueries;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

public interface Tables {

    // Pure Tables

    class Recommendations extends SCBaseTable {

        // table instance
        public static final Recommendations TABLE = new Recommendations();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column SEED_ID = Column.create(TABLE, "seed_id");
        public static final Column RECOMMENDED_SOUND_ID = Column.create(TABLE, "recommended_sound_id");
        public static final Column RECOMMENDED_SOUND_TYPE = Column.create(TABLE, "recommended_sound_type");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Recommendations (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_id INTEGER, " +
                "recommended_sound_id INTEGER," +
                "recommended_sound_type INTEGER," +
                "FOREIGN KEY(seed_id) REFERENCES RecommendationSeeds(_id) " +
                "FOREIGN KEY(recommended_sound_id, recommended_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        Recommendations() {
            super("Recommendations", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecommendationSeeds extends SCBaseTable {

        // table instance
        public static final RecommendationSeeds TABLE = new RecommendationSeeds();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column SEED_SOUND_ID = Column.create(TABLE, "seed_sound_id");
        public static final Column SEED_SOUND_TYPE = Column.create(TABLE, "seed_sound_type");
        public static final Column RECOMMENDATION_REASON = Column.create(TABLE, "recommendation_reason");
        public static final Column QUERY_POSITION = Column.create(TABLE, "query_position");
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn");

        public static final int REASON_LIKED = 0;
        public static final int REASON_PLAYED = 1;

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecommendationSeeds (" +
                "_id INTEGER PRIMARY KEY," +
                "seed_sound_id INTEGER, " +
                "seed_sound_type INTEGER, " +
                "recommendation_reason INTEGER, " +
                "query_position INTEGER, " +
                "query_urn TEXT, " +
                "FOREIGN KEY(seed_sound_id, seed_sound_type) REFERENCES Sounds(_id, _type)" +
                ");";

        RecommendationSeeds() {
            super("RecommendationSeeds", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlayQueue extends SCBaseTable {

        public static final PlayQueue TABLE = new PlayQueue();

        public static final Column ENTITY_ID = Column.create(TABLE, "entity_id");
        public static final Column ENTITY_TYPE = Column.create(TABLE, "entity_type");
        public static final Column REPOSTER_ID = Column.create(TABLE, "reposter_id");
        public static final Column RELATED_ENTITY = Column.create(TABLE, "related_entity");
        public static final Column SOURCE = Column.create(TABLE, "source");
        public static final Column SOURCE_VERSION = Column.create(TABLE, "source_version");
        public static final Column SOURCE_URN = Column.create(TABLE, "source_urn");
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn");
        public static final Column CONTEXT_TYPE = Column.create(TABLE, "context_type");
        public static final Column CONTEXT_URN = Column.create(TABLE, "context_urn");
        public static final Column CONTEXT_QUERY = Column.create(TABLE, "context_query");

        public static final int ENTITY_TYPE_TRACK = 0;
        public static final int ENTITY_TYPE_PLAYLIST = 1;

        static final String SQL = "CREATE TABLE IF NOT EXISTS PlayQueue (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "entity_id INTEGER," +
                "entity_type INTEGER," +
                "reposter_id INTEGER," +
                "related_entity TEXT," +
                "source TEXT," +
                "source_version TEXT," +
                "source_urn TEXT," +
                "query_urn TEXT," +
                "context_type TEXT," +
                "context_urn TEXT," +
                "context_query TEXT" +
                ");";

        PlayQueue() {
            super("PlayQueue", PrimaryKey.of(_ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Stations extends SCBaseTable {
        public static final Stations TABLE = new Stations();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column TYPE = Column.create(TABLE, "type");
        public static final Column TITLE = Column.create(TABLE, "title");
        public static final Column PERMALINK = Column.create(TABLE, "permalink");
        public static final Column ARTWORK_URL_TEMPLATE = Column.create(TABLE, "artwork_url_template");
        public static final Column LAST_PLAYED_TRACK_POSITION = Column.create(TABLE, "last_played_track_position");
        public static final Column PLAY_QUEUE_UPDATED_AT = Column.create(TABLE, "play_queue_updated_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Stations (" +
                "station_urn TEXT," +
                "type TEXT," +
                "title TEXT," +
                "permalink TEXT," +
                "artwork_url_template TEXT," +
                "last_played_track_position INTEGER DEFAULT NULL," +
                "play_queue_updated_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY(station_urn) ON CONFLICT REPLACE" +
                ");";

        Stations() {
            super("Stations", PrimaryKey.of("station_urn"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class StationsPlayQueues extends SCBaseTable {
        public static final StationsPlayQueues TABLE = new StationsPlayQueues();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column QUERY_URN = Column.create(TABLE, "query_urn");
        public static final Column POSITION = Column.create(TABLE, "position");

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsPlayQueues (" +
                "station_urn TEXT," +
                "track_id INTEGER," +
                "query_urn TEXT," +
                "position INTEGER DEFAULT 0," +
                "PRIMARY KEY(station_urn, track_id, position) ON CONFLICT REPLACE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        StationsPlayQueues() {
            super("StationsPlayQueues", PrimaryKey.of("station_urn", "track_id", "position"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class StationsCollections extends SCBaseTable {
        public static final StationsCollections TABLE = new StationsCollections();

        public static final Column STATION_URN = Column.create(TABLE, "station_urn");
        public static final Column COLLECTION_TYPE = Column.create(TABLE, "collection_type");
        public static final Column POSITION = Column.create(TABLE, "position");
        public static final Column ADDED_AT = Column.create(TABLE, "added_at");
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS StationsCollections (" +
                "station_urn TEXT NOT NULL," +
                "collection_type INTEGER NOT NULL," +
                "position INTEGER," +
                "added_at INTEGER," +
                "removed_at INTEGER," +
                "PRIMARY KEY(station_urn, collection_type) ON CONFLICT IGNORE," +
                "FOREIGN KEY(station_urn) REFERENCES Stations(station_urn)" +
                ");";

        StationsCollections() {
            super("StationsCollections", PrimaryKey.of("station_urn, collection_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class TrackDownloads extends SCBaseTable {

        public static final TrackDownloads TABLE = new TrackDownloads();

        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column REMOVED_AT = Column.create(TABLE, "removed_at");
        public static final Column REQUESTED_AT = Column.create(TABLE, "requested_at");
        public static final Column DOWNLOADED_AT = Column.create(TABLE, "downloaded_at");
        public static final Column UNAVAILABLE_AT = Column.create(TABLE, "unavailable_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS TrackDownloads (" +
                "_id INTEGER PRIMARY KEY," +
                "requested_at INTEGER DEFAULT CURRENT_TIMESTAMP," +
                "downloaded_at INTEGER DEFAULT NULL," +
                "removed_at INTEGER DEFAULT NULL," + // track marked for deletion
                "unavailable_at INTEGER DEFAULT NULL" +
                ");";

        TrackDownloads() {
            super("TrackDownloads", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class OfflineContent extends SCBaseTable {

        public static final OfflineContent TABLE = new OfflineContent();

        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column _TYPE = Column.create(TABLE, "_type");

        public static final int TYPE_PLAYLIST = Sounds.TYPE_PLAYLIST;
        public static final int TYPE_COLLECTION = Sounds.TYPE_COLLECTION;

        public static final int ID_OFFLINE_LIKES = 0;

        static final String SQL = "CREATE TABLE IF NOT EXISTS OfflineContent (" +
                "_id INTEGER," +
                "_type INTEGER," +
                "PRIMARY KEY (_id, _type)," +
                "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
                ");";

        OfflineContent() {
            super("OfflineContent", PrimaryKey.of(BaseColumns._ID, "_type"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Comments extends SCBaseTable {

        public static final Comments TABLE = new Comments();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID);
        public static final Column URN = Column.create(TABLE, "urn");
        public static final Column USER_ID = Column.create(TABLE, "user_id");
        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp");
        public static final Column CREATED_AT = Column.create(TABLE, "created_at");
        public static final Column BODY = Column.create(TABLE, "body");

        static final String SQL = "CREATE TABLE IF NOT EXISTS Comments (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "urn TEXT UNIQUE," +
                "user_id INTEGER," +
                "track_id INTEGER," +
                "timestamp INTEGER," +
                "created_at INTEGER," +
                "body VARCHAR(255)" +
                ");";

        Comments() {
            super("Comments", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class Charts extends SCBaseTable {

        // table instance
        public static final Charts TABLE = new Charts();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column DISPLAY_NAME = Column.create(TABLE, "display_name");
        public static final Column GENRE = Column.create(TABLE, "genre");
        public static final Column TYPE = Column.create(TABLE, "type");
        public static final Column CATEGORY = Column.create(TABLE, "category");
        public static final Column BUCKET_TYPE = Column.create(TABLE, "bucket_type");

        public static final int BUCKET_TYPE_GLOBAL = 0;
        public static final int BUCKET_TYPE_FEATURED_GENRES = 1;
        public static final int BUCKET_TYPE_ALL_GENRES = 2;

        static final String SQL = "CREATE TABLE IF NOT EXISTS Charts (" +
                "_id INTEGER PRIMARY KEY," +
                "display_name TEXT, " +
                "genre TEXT, " +
                "type TEXT, " +
                "category TEXT," +
                "bucket_type INTEGER" +
                ");";

        Charts() {
            super("Charts", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class ChartTracks extends SCBaseTable {

        // table instance
        public static final ChartTracks TABLE = new ChartTracks();
        // columns
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column CHART_ID = Column.create(TABLE, "chart_id");
        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column TRACK_ARTWORK = Column.create(TABLE, "track_artwork");
        public static final Column BUCKET_TYPE = Column.create(TABLE, "bucket_type");

        static final String SQL = "CREATE TABLE IF NOT EXISTS ChartTracks (" +
                "_id INTEGER PRIMARY KEY," +
                "chart_id INTEGER, " +
                "track_id INTEGER, " +
                "track_artwork TEXT, " +
                "bucket_type INTEGER," +
                "FOREIGN KEY(chart_id) REFERENCES Charts(_id) " +
                ");";

        ChartTracks() {
            super("ChartTracks", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlayHistory extends SCBaseTable {
        public static final PlayHistory TABLE = new PlayHistory();

        public static final Column TRACK_ID = Column.create(TABLE, "track_id");
        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp");
        public static final Column SYNCED = Column.create(TABLE, "synced");

        static final String SQL = "CREATE TABLE IF NOT EXISTS PlayHistory (" +
                "timestamp INTEGER NOT NULL," +
                "track_id INTEGER NOT NULL," +
                "synced BOOLEAN DEFAULT 0," +
                "PRIMARY KEY (timestamp, track_id)" +
                ");";

        PlayHistory() {
            super("PlayHistory", PrimaryKey.of("timestamp", "track_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class RecentlyPlayed extends SCBaseTable {
        public static final RecentlyPlayed TABLE = new RecentlyPlayed();

        public static final Column TIMESTAMP = Column.create(TABLE, "timestamp");
        public static final Column CONTEXT_TYPE = Column.create(TABLE, "context_type");
        public static final Column CONTEXT_ID = Column.create(TABLE, "context_id");
        public static final Column SYNCED = Column.create(TABLE, "synced");

        static final String SQL = "CREATE TABLE IF NOT EXISTS RecentlyPlayed (" +
                "timestamp INTEGER NOT NULL," +
                "context_type INTEGER NOT NULL," +
                "context_id INTEGER NOT NULL," +
                "synced BOOLEAN DEFAULT 0," +
                "PRIMARY KEY (timestamp, context_type, context_id)" +
                ");";

        static final String MIGRATE_SQL = "INSERT OR IGNORE INTO RecentlyPlayed " +
                "(timestamp, context_type, context_id) " +
                "SELECT timestamp, context_type, context_id " +
                "FROM PlayHistory WHERE context_type != 0;";

        RecentlyPlayed() {
            super("RecentlyPlayed", PrimaryKey.of("timestamp", "context_type", "context_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class SuggestedCreators extends SCBaseTable {
        public static final SuggestedCreators TABLE = new SuggestedCreators();

        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column SEED_USER_ID = Column.create(TABLE, "seed_user_id");
        public static final Column SUGGESTED_USER_ID = Column.create(TABLE, "suggested_user_id");
        public static final Column RELATION_KEY = Column.create(TABLE, "relation_key");
        public static final Column FOLLOWED_AT = Column.create(TABLE, "followed_at");

        static final String SQL = "CREATE TABLE IF NOT EXISTS SuggestedCreators (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "seed_user_id INTEGER, " +
                "suggested_user_id INTEGER, " +
                "relation_key VARCHAR(50), " +
                "followed_at INTEGER," +
                "FOREIGN KEY(seed_user_id) REFERENCES Users(_id) " +
                "FOREIGN KEY(suggested_user_id) REFERENCES Users(_id) " +
                ");";

        SuggestedCreators() {
            super("SuggestedCreators", PrimaryKey.of("_id"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    // Views

    class SearchSuggestions extends SCBaseTable {

        public static final SearchSuggestions TABLE = new SearchSuggestions();

        public static final Column KIND = Column.create(TABLE, "kind");
        public static final Column _ID = Column.create(TABLE, "_id");
        public static final Column _TYPE = Column.create(TABLE, "_type");
        public static final Column DISPLAY_TEXT = Column.create(TABLE, "display_text");
        public static final Column IMAGE_URL = Column.create(TABLE, "image_url");

        public static final String KIND_LIKE = "like";
        public static final String KIND_FOLLOWING = "following";

        static final String SQL = "CREATE VIEW IF NOT EXISTS SearchSuggestions AS SELECT '" +
                KIND_LIKE + "' AS kind, " +
                "Sounds._id AS _id, " +
                "Sounds._type AS _type, " +
                "title AS display_text, " +
                "artwork_url AS image_url " +
                "FROM Likes INNER JOIN Sounds ON Likes._id = Sounds._id " +
                "AND Likes._type = Sounds._type" +
                " UNION" +
                " SELECT '" + KIND_FOLLOWING + "' AS kind, " +
                "Users._id, 0 AS _type, " +
                "username AS text, " +
                "avatar_url AS image_url " +
                "from UserAssociations INNER JOIN Users ON UserAssociations.target_id = Users._id";

        SearchSuggestions() {
            super("SearchSuggestions", PrimaryKey.of(BaseColumns._ID, "kind"));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class OfflinePlaylistTracks extends SCBaseTable {

        public static final OfflinePlaylistTracks TABLE = new OfflinePlaylistTracks();

        public static final Column _ID = Column.create(TABLE, BaseColumns._ID);
        public static final Column _TYPE = Column.create(TABLE, "_type");
        public static final Column USER_ID = Column.create(TABLE, "user_id");
        public static final Column DURATION = Column.create(TABLE, "duration");
        public static final Column WAVEFORM_URL = Column.create(TABLE, "waveform_url");
        public static final Column ARTWORK_URL = Column.create(TABLE, "artwork_url");
        public static final Column SYNCABLE = Column.create(TABLE, "syncable");
        public static final Column SNIPPED = Column.create(TABLE, "snipped");
        public static final Column LAST_POLICY_UPDATE = Column.create(TABLE, "last_policy_update");
        public static final Column CREATED_AT = Column.create(TABLE, "created_at");
        public static final Column POSITION = Column.create(TABLE, "position");

        static final String SQL = "CREATE VIEW IF NOT EXISTS OfflinePlaylistTracks AS " +
                "SELECT " +
                "Sounds._id as _id," +
                "Sounds._type as _type, " +
                "Sounds.user_id as user_id, " +
                "Sounds.full_duration as duration, " +
                "Sounds.waveform_url as waveform_url, " +
                "Sounds.artwork_url as artwork_url, " +
                "TrackPolicies.syncable as syncable, " +
                "TrackPolicies.snipped as snipped, " +
                "TrackPolicies.last_updated as last_policy_update, " +
                "PlaylistTracks.position as position, " +
                "MAX(IFNULL(PlaylistLikes.created_at, 0), PlaylistProperties.created_at ) AS created_at " +
                // ^ The timestamp used to sort
                "FROM Sounds " +
                "INNER JOIN PlaylistTracks ON Sounds._id = PlaylistTracks.track_id " +
                // ^ Add PlaylistTracks to tracks
                "LEFT JOIN Likes as PlaylistLikes ON (PlaylistTracks.playlist_id = PlaylistLikes._id) AND (PlaylistLikes._type = " + TableColumns.Sounds.TYPE_PLAYLIST + ") " +
                // ^ When available, adds the Playlist Like date to the tracks (for sorting purpose)
                "LEFT JOIN Sounds as PlaylistProperties ON (PlaylistProperties._id = PlaylistTracks.playlist_id AND PlaylistProperties._type = " + TableColumns.Sounds.TYPE_PLAYLIST + ")" +
                // ^ Add the playlist creation date
                "INNER JOIN OfflineContent ON PlaylistTracks.playlist_id = OfflineContent._id  AND Sounds._type = " + TableColumns.Sounds.TYPE_TRACK + " " +
                // ^ Keep only offline tracks
                "INNER JOIN TrackPolicies ON PlaylistTracks.track_id = TrackPolicies.track_id " +
                // ^ Keep only tracks with policies
                "WHERE (PlaylistTracks.removed_at IS NULL) ";

        OfflinePlaylistTracks() {
            super("OfflinePlaylistTracks", PrimaryKey.of(BaseColumns._ID));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class PlaylistView extends SCBaseTable {

        public static final PlaylistView TABLE = new PlaylistView();

        PlaylistView() {
            super("PlaylistView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "pv_id");
        public static final Column TITLE = Column.create(TABLE, "pv_title");
        public static final Column USERNAME = Column.create(TABLE, "pv_username");
        public static final Column USER_ID = Column.create(TABLE, "pv_user_id");
        public static final Column TRACK_COUNT = Column.create(TABLE, "pv_track_count");
        public static final Column LIKES_COUNT = Column.create(TABLE, "pv_likes_count");
        public static final Column SHARING = Column.create(TABLE, "pv_sharing");
        public static final Column ARTWORK_URL = Column.create(TABLE, "pv_artwork_url");
        public static final Column GENRE = Column.create(TABLE, "pv_genre");
        public static final Column TAG_LIST = Column.create(TABLE, "pv_tag_list");
        public static final Column LOCAL_TRACK_COUNT = Column.create(TABLE, "pv_local_track_count");
        public static final Column HAS_PENDING_DOWNLOAD_REQUEST = Column.create(TABLE, "pv_has_pending_download_request");
        public static final Column HAS_DOWNLOADED_TRACKS = Column.create(TABLE, "pv_has_downloaded_tracks");
        public static final Column HAS_UNAVAILABLE_TRACKS = Column.create(TABLE, "pv_has_unavailable_tracks");
        public static final Column IS_MARKED_FOR_OFFLINE = Column.create(TABLE, "pv_is_marked_for_offline");
        public static final Column IS_USER_LIKE = Column.create(TABLE, "pv_is_user_like");
        public static final Column IS_ALBUM = Column.create(TABLE, "pv_is_album");

        static final String SQL = "CREATE VIEW IF NOT EXISTS PlaylistView AS " +
                Query.from(SoundView.name())
                     .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                             field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                             field(SoundView.field(TableColumns.SoundView.USERNAME)).as(USERNAME.name()),
                             field(SoundView.field(TableColumns.SoundView.USER_ID)).as(USER_ID.name()),
                             field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TRACK_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),
                             field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                             field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),
                             field(SoundView.field(TableColumns.SoundView.IS_ALBUM)).as(IS_ALBUM.name()),
                             field("(" + PlaylistQueries.LOCAL_TRACK_COUNT.build() + ")").as(LOCAL_TRACK_COUNT.name()),
                             exists(likeQuery()).as(IS_USER_LIKE.name()),
                             exists(PlaylistQueries.HAS_PENDING_DOWNLOAD_REQUEST_QUERY).as(HAS_PENDING_DOWNLOAD_REQUEST.name()),
                             exists(PlaylistQueries.HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER).as(HAS_DOWNLOADED_TRACKS.name()),
                             exists(PlaylistQueries.HAS_UNAVAILABLE_OFFLINE_TRACKS_FILTER).as(HAS_UNAVAILABLE_TRACKS.name()),
                             exists(PlaylistQueries.IS_MARKED_FOR_OFFLINE_QUERY).as(IS_MARKED_FOR_OFFLINE.name()))
                     .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Sounds.TYPE_PLAYLIST);


        static Query likeQuery() {
            final Where joinConditions = filter()
                    .whereEq(Table.SoundView.field(Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                    .whereEq(Table.SoundView.field(Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

            return Query.from(Table.Likes.name())
                    // do not use SoundView here. The exists query will fail, in spite of passing tests
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class UsersView extends SCBaseTable {

        public static final UsersView TABLE = new UsersView();

        UsersView() {
            super("UsersView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "uv_id");
        public static final Column USERNAME = Column.create(TABLE, "uv_username");
        public static final Column COUNTRY = Column.create(TABLE, "uv_country");
        public static final Column CITY = Column.create(TABLE, "uv_city");
        public static final Column FOLLOWERS_COUNT = Column.create(TABLE, "uv_followers_count");
        public static final Column DESCRIPTION = Column.create(TABLE, "uv_description");
        public static final Column AVATAR_URL = Column.create(TABLE, "uv_avatar_url");
        public static final Column VISUAL_URL = Column.create(TABLE, "uv_visual_url");
        public static final Column WEBSITE_URL = Column.create(TABLE, "uv_website_url");
        public static final Column WEBSITE_NAME = Column.create(TABLE, "uv_website_name");
        public static final Column MYSPACE_NAME = Column.create(TABLE, "uv_myspace_name");
        public static final Column DISCOGS_NAME = Column.create(TABLE, "uv_discogs_name");
        public static final Column ARTIST_STATION = Column.create(TABLE, "uv_artist_station");
        public static final Column IS_FOLLOWING = Column.create(TABLE, "uv_is_following");


        static final String SQL = "CREATE VIEW IF NOT EXISTS UsersView AS " +
                Query.from(Table.Users)
                        .select(
                                field(Table.Users.field(TableColumns.Users._ID)).as(ID.name()),
                                field(Table.Users.field(TableColumns.Users.USERNAME)).as(USERNAME.name()),
                                field(Table.Users.field(TableColumns.Users.COUNTRY)).as(COUNTRY.name()),
                                field(Table.Users.field(TableColumns.Users.CITY)).as(CITY.name()),
                                field(Table.Users.field(TableColumns.Users.FOLLOWERS_COUNT)).as(FOLLOWERS_COUNT.name()),
                                field(Table.Users.field(TableColumns.Users.DESCRIPTION)).as(DESCRIPTION.name()),
                                field(Table.Users.field(TableColumns.Users.AVATAR_URL)).as(AVATAR_URL.name()),
                                field(Table.Users.field(TableColumns.Users.VISUAL_URL)).as(VISUAL_URL.name()),
                                field(Table.Users.field(TableColumns.Users.WEBSITE_URL)).as(WEBSITE_URL.name()),
                                field(Table.Users.field(TableColumns.Users.WEBSITE_NAME)).as(WEBSITE_NAME.name()),
                                field(Table.Users.field(TableColumns.Users.MYSPACE_NAME)).as(MYSPACE_NAME.name()),
                                field(Table.Users.field(TableColumns.Users.DISCOGS_NAME)).as(DISCOGS_NAME.name()),
                                field(Table.Users.field(TableColumns.Users.ARTIST_STATION)).as(ARTIST_STATION.name()),
                                exists(followingQuery()).as(IS_FOLLOWING.name())
                        );

        static Query followingQuery() {
            return Query.from(Table.UserAssociations.name())
                    .whereEq(Table.Users.field(TableColumns.Users._ID),
                            Table.UserAssociations.field(TableColumns.UserAssociations.TARGET_ID))
                    .whereNull(TableColumns.UserAssociations.REMOVED_AT);
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

    class TrackView extends SCBaseTable {

        public static final TrackView TABLE = new TrackView();

        TrackView() {
            super("TrackView", PrimaryKey.of(_ID));
        }

        public static final Column ID = Column.create(TABLE, "tv_id");
        public static final Column CREATED_AT = Column.create(TABLE, "tv_created_at");
        public static final Column TITLE = Column.create(TABLE, "tv_title");
        public static final Column USERNAME = Column.create(TABLE, "tv_username");
        public static final Column USER_ID = Column.create(TABLE, "tv_user_id");
        public static final Column PERMALINK_URL = Column.create(TABLE, "tv_permalink_url");
        public static final Column WAVEFORM_URL = Column.create(TABLE, "tv_waveform_url");

        public static final Column SNIPPET_DURATION = Column.create(TABLE, "tv_snippet_duration");
        public static final Column FULL_DURATION = Column.create(TABLE, "tv_full_duration");

        public static final Column PLAY_COUNT = Column.create(TABLE, "tv_play_count");
        public static final Column LIKES_COUNT = Column.create(TABLE, "tv_likes_count");
        public static final Column REPOSTS_COUNT = Column.create(TABLE, "tv_reposts_count");
        public static final Column COMMENTS_COUNT = Column.create(TABLE, "tv_comments_count");
        public static final Column IS_COMMENTABLE = Column.create(TABLE, "tv_is_commentable");
        public static final Column GENRE = Column.create(TABLE, "tv_genre");
        public static final Column TAG_LIST = Column.create(TABLE, "tv_tag_list");
        public static final Column SHARING = Column.create(TABLE, "tv_sharing");
        public static final Column POLICY = Column.create(TABLE, "tv_policy");
        public static final Column MONETIZABLE = Column.create(TABLE, "tv_monetizable");
        public static final Column MONETIZATION_MODEL = Column.create(TABLE, "tv_monetization_model");
        public static final Column BLOCKED = Column.create(TABLE, "tv_blocked");
        public static final Column SNIPPED = Column.create(TABLE, "tv_snipped");
        public static final Column SUB_HIGH_TIER = Column.create(TABLE, "tv_sub_high_tier");

        public static final Column ARTWORK_URL = Column.create(TABLE, "tv_artwork_url");
        public static final Column IS_USER_LIKE = Column.create(TABLE, "tv_is_user_like");
        public static final Column IS_USER_REPOST = Column.create(TABLE, "tv_is_user_repost");

        public static final Column OFFLINE_REMOVED_AT = Column.create(TABLE, "tv_offline_removed_at");
        public static final Column OFFLINE_DOWNLOADED_AT = Column.create(TABLE, "tv_offline_downloaded_at");

        static final String SQL = "CREATE VIEW IF NOT EXISTS TrackView AS " +
                Query.from(SoundView.name())
                     .select(field(SoundView.field(TableColumns.SoundView._ID)).as(ID.name()),
                             field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(CREATED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.TITLE)).as(TITLE.name()),
                             field(SoundView.field(TableColumns.SoundView.USERNAME)).as(USERNAME.name()),
                             field(SoundView.field(TableColumns.SoundView.USER_ID)).as(USER_ID.name()),
                             field(SoundView.field(TableColumns.SoundView.PERMALINK_URL)).as(PERMALINK_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.WAVEFORM_URL)).as(WAVEFORM_URL.name()),
                             field(SoundView.field(TableColumns.SoundView.SNIPPET_DURATION)).as(SNIPPET_DURATION.name()),
                             field(SoundView.field(TableColumns.SoundView.FULL_DURATION)).as(FULL_DURATION.name()),

                             field(SoundView.field(TableColumns.SoundView.GENRE)).as(GENRE.name()),
                             field(SoundView.field(TableColumns.SoundView.TAG_LIST)).as(TAG_LIST.name()),

                             field(SoundView.field(TableColumns.SoundView.PLAYBACK_COUNT)).as(PLAY_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(LIKES_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.REPOSTS_COUNT)).as(REPOSTS_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.COMMENT_COUNT)).as(COMMENTS_COUNT.name()),
                             field(SoundView.field(TableColumns.SoundView.COMMENTABLE)).as(IS_COMMENTABLE.name()),
                             field(SoundView.field(TableColumns.SoundView.SHARING)).as(SHARING.name()),

                             field(SoundView.field(TableColumns.SoundView.POLICIES_POLICY)).as(POLICY.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZABLE)).as(MONETIZABLE.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_MONETIZATION_MODEL)).as(MONETIZATION_MODEL.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_BLOCKED)).as(BLOCKED.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_SNIPPED)).as(SNIPPED.name()),
                             field(SoundView.field(TableColumns.SoundView.POLICIES_SUB_HIGH_TIER)).as(SUB_HIGH_TIER.name()),

                             field(SoundView.field(TableColumns.SoundView.OFFLINE_DOWNLOADED_AT)).as(OFFLINE_DOWNLOADED_AT.name()),
                             field(SoundView.field(TableColumns.SoundView.OFFLINE_REMOVED_AT)).as(OFFLINE_REMOVED_AT.name()),

                             field(SoundView.field(TableColumns.SoundView.ARTWORK_URL)).as(ARTWORK_URL.name()),

                             exists(likeQuery()).as(IS_USER_LIKE.name()),
                             exists(repostQuery()).as(IS_USER_REPOST.name()))

                     .whereEq(SoundView.field(TableColumns.SoundView._TYPE), Sounds.TYPE_TRACK);

        static Query likeQuery() {
            final Where joinConditions = filter()
                    .whereEq(Table.SoundView.field(Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                    .whereEq(Table.SoundView.field(Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

            return Query.from(Table.Likes.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
        }

        static Query repostQuery() {
            final Where joinConditions = Filter.filter()
                    .whereEq(Table.SoundView.field(_ID), TableColumns.Posts.TARGET_ID)
                    .whereEq(Table.SoundView.field(_TYPE), TableColumns.Posts.TARGET_TYPE);

            return Query.from(Table.Posts.name())
                    .innerJoin(Table.Sounds.name(), joinConditions)
                    .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), typeRepostDelimited());
        }

        private static String typeRepostDelimited() {
            return "'" + TableColumns.Posts.TYPE_REPOST + "'";
        }

        @Override
        String getCreateSQL() {
            return SQL;
        }
    }

}
