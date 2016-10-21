package com.soundcloud.android.storage;

import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.api.legacy.model.Playable;

// I have an idea for how to generate these things going forward, so let's not spend
// time on improving the string building mess here.
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Deprecated // use the new `Tables` structure
final class DatabaseSchema {

    static final String DATABASE_CREATE_SOUNDSTREAM = "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "sound_id INTEGER, " +
            "sound_type INTEGER," +
            "reposter_id INTEGER," +
            "promoted_id INTEGER," +
            "created_at INTEGER," +
            "FOREIGN KEY(sound_id) REFERENCES Sounds(_id) " +
            ");";

    static final String DATABASE_CREATE_PROMOTED_TRACKS = "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "created_at INTEGER," +
            "ad_urn TEXT, " +
            "promoter_id INTEGER," +
            "promoter_name TEXT," +
            "tracking_track_clicked_urls TEXT," +
            "tracking_profile_clicked_urls TEXT," +
            "tracking_promoter_clicked_urls TEXT," +
            "tracking_track_played_urls TEXT," +
            "tracking_track_impression_urls TEXT" +
            ");";

    static final String DATABASE_CREATE_SOUNDS = "(" +
            "_id INTEGER," +
            "_type INTEGER," +
            "last_updated INTEGER," +
            "permalink VARCHAR(255)," +
            "original_content_size INTEGER," +
            "duration INTEGER," +
            "snippet_duration INTEGER," +
            "full_duration INTEGER," +
            "state VARCHAR(50)," +
            "created_at INTEGER," +
            "genre VARCHAR(100)," +
            "tag_list VARCHAR(500)," +
            "track_type VARCHAR(255)," +
            "title VARCHAR(255)," +
            "permalink_url VARCHAR(255)," +
            "artwork_url VARCHAR(255), " +
            "waveform_url VARCHAR(255), " +
            "downloadable BOOLEAN, " +
            "commentable BOOLEAN, " +
            "download_url VARCHAR(255), " +
            "stream_url VARCHAR(255)," +
            "streamable BOOLEAN DEFAULT 0, " +
            "sharing VARCHAR(255)," +
            "license VARCHAR(100)," +
            "purchase_url VARCHAR(255)," +
            "playback_count INTEGER DEFAULT -1," +
            "download_count INTEGER DEFAULT -1," +
            "comment_count INTEGER DEFAULT -1," +
            "favoritings_count INTEGER DEFAULT -1," +
            "reposts_count INTEGER DEFAULT -1," +
            "shared_to_count INTEGER DEFAULT -1," +
            "sharing_note_text VARCHAR(255)," +
            "tracks_uri VARCHAR(255)," +
            "track_count INTEGER DEFAULT -1," +
            "playlist_type VARCHAR(255)," +
            "user_id INTEGER," +
            "removed_at INTEGER DEFAULT NULL," +
            "modified_at INTEGER DEFAULT NULL," +
            "DESCRIPTION TEXT," +
            "is_album BOOLEAN DEFAULT 0," +
            "set_type VARCHAR(255)," +
            "release_date VARCHAR(255)," +
            "PRIMARY KEY (_id, _type) ON CONFLICT IGNORE" +
            ");";

    static final String DATABASE_CREATE_WAVEFORMS = "(" +
            "track_id INTEGER, " +
            "max_amplitude INTEGER, " +
            "samples TEXT, " +
            "created_at INTEGER," +
            "PRIMARY KEY (track_id) ON CONFLICT REPLACE " +
            ");";

    static final String DATABASE_CREATE_TRACK_POLICIES = "(" +
            "track_id INTEGER, " +
            "monetizable BOOLEAN DEFAULT 0," +
            "blocked BOOLEAN DEFAULT 0," +
            "snipped BOOLEAN DEFAULT 0," +
            "syncable BOOLEAN DEFAULT 1," +
            "sub_mid_tier BOOLEAN DEFAULT 0," +
            "sub_high_tier BOOLEAN DEFAULT 0," +
            "policy TEXT," +
            "monetization_model TEXT," +
            "last_updated INTEGER, " +
            "PRIMARY KEY (track_id) ON CONFLICT REPLACE " +
            ");";

    static final String DATABASE_CREATE_PLAYLIST_TRACKS = "(" +
            "playlist_id INTEGER, " +
            "track_id INTEGER," +
            "position INTEGER," +
            "added_at INTEGER," +
            "removed_at INTEGER," +
            "PRIMARY KEY (track_id, position, playlist_id) ON CONFLICT IGNORE" +
            ");";

    static final String DATABASE_CREATE_USERS = "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "_type INTEGER DEFAULT 0," +

            // mini representation
            "username VARCHAR(255)," +
            "avatar_url VARCHAR(255)," +
            "permalink VARCHAR(255)," +
            "permalink_url VARCHAR(255)," +

            "full_name VARCHAR(255)," +
            "description text," +
            "city VARCHAR(255)," +
            "country VARCHAR(255)," +

            "artist_station TEXT," +
            "plan VARCHAR(16)," +
            "primary_email_confirmed INTEGER," +

            "website VARCHAR(255)," +
            "website_title VARCHAR(255), " +

            "discogs_name VARCHAR(255)," +
            "myspace_name VARCHAR(255)," +

            "visual_url VARCHAR(255)," +

            // counts
            "track_count INTEGER DEFAULT -1," +
            "followers_count INTEGER DEFAULT -1," +
            "followings_count INTEGER DEFAULT -1," +
            "public_favorites_count INTEGER DEFAULT -1," +
            "private_tracks_count INTEGER DEFAULT -1," +

            // internal
            "last_updated INTEGER" +
            ");";

    static final String DATABASE_CREATE_ACTIVITIES = "(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid VARCHAR(255)," +
            "user_id INTEGER," +
            "sound_id INTEGER," +
            "sound_type INTEGER," +
            "comment_id INTEGER," +
            "type String," +
            "tags VARCHAR(255)," +
            "created_at INTEGER," +
            "content_id INTEGER," +
            "sharing_note_text VARCHAR(255)," +
            "sharing_note_created_at INTEGER," +
            "UNIQUE (created_at, type, content_id, sound_id, user_id)" +
            ");";

    /**
     * {@link com.soundcloud.android.storage.TableColumns.Collections}
     */
    static final String DATABASE_CREATE_COLLECTIONS = "(" +
            "uri TEXT PRIMARY KEY," +
            "last_sync INTEGER DEFAULT 0, " +
            "last_sync_attempt INTEGER DEFAULT 0, " +
            "extra TEXT" +
            ");";

    static final String DATABASE_CREATE_LIKES = "(" +
            "_id INTEGER NOT NULL," +
            "_type INTEGER NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "added_at INTEGER DEFAULT NULL," +
            "removed_at INTEGER DEFAULT NULL," +
            "PRIMARY KEY (_id, _type)," +
            "FOREIGN KEY(_id, _type) REFERENCES Sounds(_id, _type)" +
            ");";

    static final String DATABASE_CREATE_POSTS = "(" +
            "type STRING NOT NULL," +
            "target_id INTEGER NOT NULL," +
            "target_type INTEGER NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "added_at INTEGER DEFAULT NULL," +
            "removed_at INTEGER DEFAULT NULL," +
            "PRIMARY KEY (type, target_id, target_type)," +
            "FOREIGN KEY(target_id, target_type) REFERENCES Sounds(_id, _type)" +
            ");";

    /**
     * {@link com.soundcloud.android.storage.TableColumns.UserAssociations}
     */
    static final String DATABASE_CREATE_USER_ASSOCIATIONS = "(" +
            "target_id INTEGER," +                  // the target user of the association
            "association_type INTEGER, " +          // the type of association (e.g. Following, Follower)
            "resource_type INTEGER DEFAULT 0, " +   // currently unused, but if we add groups...
            "position INTEGER, " +                  // as returned from the api
            "created_at INTEGER, " +                // indicates when this was created on the api
            "added_at INTEGER, " +                  // when was this added locally (pre-api sync)
            "removed_at INTEGER, " +                // when was this removed locally (pre-api sync)
            "token VARCHAR(150), " +                // whitelist token to avoid spam flagging. comes from API
            "PRIMARY KEY(target_id, association_type, resource_type) ON CONFLICT REPLACE" +
            ");";

    static final String DATABASE_CREATE_SOUND_VIEW = "AS SELECT " +
            "Sounds." + TableColumns.Sounds._ID + " as " + TableColumns.SoundView._ID +
            ",Sounds." + TableColumns.Sounds._TYPE + " as " + TableColumns.SoundView._TYPE +
            ",Sounds." + TableColumns.Sounds.LAST_UPDATED + " as " + TableColumns.SoundView.LAST_UPDATED +
            ",Sounds." + TableColumns.Sounds.PERMALINK + " as " + TableColumns.SoundView.PERMALINK +
            ",Sounds." + TableColumns.Sounds.CREATED_AT + " as " + TableColumns.SoundView.CREATED_AT +
            ",Sounds." + TableColumns.Sounds.DURATION + " as " + TableColumns.SoundView.DURATION +
            ",Sounds." + TableColumns.Sounds.SNIPPET_DURATION + " as " + TableColumns.SoundView.SNIPPET_DURATION +
            ",Sounds." + TableColumns.Sounds.FULL_DURATION + " as " + TableColumns.SoundView.FULL_DURATION +
            ",Sounds." + TableColumns.Sounds.ORIGINAL_CONTENT_SIZE + " as " + TableColumns.SoundView.ORIGINAL_CONTENT_SIZE +
            ",Sounds." + TableColumns.Sounds.STATE + " as " + TableColumns.SoundView.STATE +
            ",Sounds." + TableColumns.Sounds.GENRE + " as " + TableColumns.SoundView.GENRE +
            ",Sounds." + TableColumns.Sounds.TAG_LIST + " as " + TableColumns.SoundView.TAG_LIST +
            ",Sounds." + TableColumns.Sounds.TRACK_TYPE + " as " + TableColumns.SoundView.TRACK_TYPE +
            ",Sounds." + TableColumns.Sounds.TITLE + " as " + TableColumns.SoundView.TITLE +
            ",Sounds." + TableColumns.Sounds.PERMALINK_URL + " as " + TableColumns.SoundView.PERMALINK_URL +
            ",Sounds." + TableColumns.Sounds.ARTWORK_URL + " as " + TableColumns.SoundView.ARTWORK_URL +
            ",Sounds." + TableColumns.Sounds.WAVEFORM_URL + " as " + TableColumns.SoundView.WAVEFORM_URL +
            ",Sounds." + TableColumns.Sounds.DOWNLOADABLE + " as " + TableColumns.SoundView.DOWNLOADABLE +
            ",Sounds." + TableColumns.Sounds.DOWNLOAD_URL + " as " + TableColumns.SoundView.DOWNLOAD_URL +
            ",Sounds." + TableColumns.Sounds.STREAM_URL + " as " + TableColumns.SoundView.STREAM_URL +
            ",Sounds." + TableColumns.Sounds.STREAMABLE + " as " + TableColumns.SoundView.STREAMABLE +
            ",Sounds." + TableColumns.Sounds.COMMENTABLE + " as " + TableColumns.SoundView.COMMENTABLE +
            ",Sounds." + TableColumns.Sounds.SHARING + " as " + TableColumns.SoundView.SHARING +
            ",Sounds." + TableColumns.Sounds.LICENSE + " as " + TableColumns.SoundView.LICENSE +
            ",Sounds." + TableColumns.Sounds.PURCHASE_URL + " as " + TableColumns.SoundView.PURCHASE_URL +
            ",Sounds." + TableColumns.Sounds.PLAYBACK_COUNT + " as " + TableColumns.SoundView.PLAYBACK_COUNT +
            ",Sounds." + TableColumns.Sounds.DOWNLOAD_COUNT + " as " + TableColumns.SoundView.DOWNLOAD_COUNT +
            ",Sounds." + TableColumns.Sounds.COMMENT_COUNT + " as " + TableColumns.SoundView.COMMENT_COUNT +
            ",Sounds." + TableColumns.Sounds.LIKES_COUNT + " as " + TableColumns.SoundView.LIKES_COUNT +
            ",Sounds." + TableColumns.Sounds.REPOSTS_COUNT + " as " + TableColumns.SoundView.REPOSTS_COUNT +
            ",Sounds." + TableColumns.Sounds.SHARED_TO_COUNT + " as " + TableColumns.SoundView.SHARED_TO_COUNT +
            ",Sounds." + TableColumns.Sounds.TRACKS_URI + " as " + TableColumns.SoundView.TRACKS_URI +
            ",Sounds." + TableColumns.Sounds.TRACK_COUNT + " as " + TableColumns.SoundView.TRACK_COUNT +
            ",Sounds." + TableColumns.Sounds.DESCRIPTION + " as " + TableColumns.SoundView.DESCRIPTION +
            ",Sounds." + TableColumns.Sounds.IS_ALBUM + " as " + TableColumns.SoundView.IS_ALBUM +
            ",Sounds." + TableColumns.Sounds.SET_TYPE + " as " + TableColumns.SoundView.SET_TYPE +
            ",Sounds." + TableColumns.Sounds.RELEASE_DATE + " as " + TableColumns.SoundView.RELEASE_DATE +
            ",TrackPolicies." + TableColumns.TrackPolicies.MONETIZABLE + " as " + TableColumns.SoundView.POLICIES_MONETIZABLE +
            ",TrackPolicies." + TableColumns.TrackPolicies.BLOCKED + " as " + TableColumns.SoundView.POLICIES_BLOCKED +
            ",TrackPolicies." + TableColumns.TrackPolicies.SNIPPED + " as " + TableColumns.SoundView.POLICIES_SNIPPED +
            ",TrackPolicies." + TableColumns.TrackPolicies.POLICY + " as " + TableColumns.SoundView.POLICIES_POLICY +
            ",TrackPolicies." + TableColumns.TrackPolicies.SYNCABLE + " as " + TableColumns.SoundView.POLICIES_SYNCABLE +
            ",TrackPolicies." + TableColumns.TrackPolicies.SUB_MID_TIER + " as " + TableColumns.SoundView.POLICIES_SUB_MID_TIER +
            ",TrackPolicies." + TableColumns.TrackPolicies.SUB_HIGH_TIER+ " as " + TableColumns.SoundView.POLICIES_SUB_HIGH_TIER +
            ",TrackPolicies." + TableColumns.TrackPolicies.MONETIZATION_MODEL + " as " + TableColumns.SoundView.POLICIES_MONETIZATION_MODEL +
            ",Users." + TableColumns.Users._ID + " as " + TableColumns.SoundView.USER_ID +
            ",Users." + TableColumns.Users.USERNAME + " as " + TableColumns.SoundView.USERNAME +
            ",Users." + TableColumns.Users.PERMALINK + " as " + TableColumns.SoundView.USER_PERMALINK +
            ",Users." + TableColumns.Users.AVATAR_URL + " as " + TableColumns.SoundView.USER_AVATAR_URL +
            "," + Tables.TrackDownloads.DOWNLOADED_AT + " as " + TableColumns.SoundView.OFFLINE_DOWNLOADED_AT +
            "," + Tables.TrackDownloads.REMOVED_AT + " as " + TableColumns.SoundView.OFFLINE_REMOVED_AT +
            " FROM Sounds" +
            " LEFT JOIN Users ON(" +
            "   Sounds." + TableColumns.Sounds.USER_ID + " = " + "Users." + TableColumns.Users._ID + ")" +
            " LEFT OUTER JOIN TrackDownloads " +
            "   ON (Sounds." + TableColumns.Sounds._ID + " = " + Tables.TrackDownloads._ID + " AND " +
            "   Sounds." + TableColumns.Sounds._TYPE + " = " + TableColumns.Sounds.TYPE_TRACK + ")" +
            " LEFT OUTER JOIN TrackPolicies ON(" +
            "   Sounds." + TableColumns.Sounds._ID + " = " + "TrackPolicies." + TableColumns.TrackPolicies.TRACK_ID + ")" +
            " WHERE " + "Sounds." + TableColumns.Sounds.REMOVED_AT + " IS NULL" +
            " AND (Sounds." + TableColumns.Sounds._TYPE + " != " + TableColumns.Sounds.TYPE_TRACK +
            " OR TrackPolicies." + TableColumns.TrackPolicies.TRACK_ID + " IS NOT NULL)";

    static final String DATABASE_CREATE_USER_ASSOCIATION_VIEW = " AS SELECT " +
            "UserAssociations." + TableColumns.UserAssociations.CREATED_AT + " as " + TableColumns.UserAssociationView.USER_ASSOCIATION_TIMESTAMP +
            ", UserAssociations." + TableColumns.UserAssociations.ASSOCIATION_TYPE + " as " + TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE +
            ", UserAssociations." + TableColumns.UserAssociations.ADDED_AT + " as " + TableColumns.UserAssociationView.USER_ASSOCIATION_ADDED_AT +
            ", UserAssociations." + TableColumns.UserAssociations.REMOVED_AT + " as " + TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT +
            ", UserAssociations." + TableColumns.UserAssociations.TOKEN + " as " + TableColumns.UserAssociationView.USER_ASSOCIATION_TOKEN +

            // user data
            ", Users.*" +
            " FROM " + Table.UserAssociations.name() + " " +
            " LEFT JOIN Users ON(" +
            "   " + Table.UserAssociations.name() + "." + TableColumns.UserAssociations.TARGET_ID + " = " + Table.Users.name() + "." + TableColumns.Users._ID + ")" +
            // this is the default position as returned by the server, which is ordered by last active users (subject to change)
            " ORDER BY " + TableColumns.UserAssociations.POSITION + " ASC";

    /**
     * A view which aggregates playlist members from the sounds and playlist_tracks table
     */
    static final String DATABASE_CREATE_PLAYLIST_TRACKS_VIEW = "AS SELECT " +
            "PlaylistTracks." + TableColumns.PlaylistTracks.PLAYLIST_ID + " as " + TableColumns.PlaylistTracksView.PLAYLIST_ID +
            ", PlaylistTracks." + TableColumns.PlaylistTracks.POSITION + " as " + TableColumns.PlaylistTracksView.PLAYLIST_POSITION +
            ", PlaylistTracks." + TableColumns.PlaylistTracks.ADDED_AT + " as " + TableColumns.PlaylistTracksView.PLAYLIST_ADDED_AT +

            // track+user data
            ", SoundView.*" +

            " FROM PlaylistTracks" +
            " INNER JOIN SoundView ON(" +
            "  PlaylistTracks." + TableColumns.PlaylistTracks.TRACK_ID + " = " + "SoundView." + TableColumns.SoundView._ID +
            " AND SoundView." + TableColumns.SoundView._TYPE + " = " + Playable.DB_TYPE_TRACK + ")";

    /**
     * A view which combines SoundStream data + tracks/users/comments
     */
    static final String DATABASE_CREATE_SOUNDSTREAM_VIEW = "AS SELECT " +
            "SoundStream." + TableColumns.SoundStream._ID + " as " + TableColumns.SoundStreamView._ID +
            ",SoundStream." + TableColumns.SoundStream.CREATED_AT + " as " + TableColumns.SoundStreamView.CREATED_AT +
            ",SoundStream." + TableColumns.SoundStream.SOUND_ID + " as " + TableColumns.SoundStreamView.SOUND_ID +
            ",SoundStream." + TableColumns.SoundStream.SOUND_TYPE + " as " + TableColumns.SoundStreamView.SOUND_TYPE +
            ",SoundStream." + TableColumns.SoundStream.REPOSTER_ID + " as " + TableColumns.SoundStreamView.REPOSTER_ID +
            ",SoundStream." + TableColumns.SoundStream.PROMOTED_ID + " as " + TableColumns.SoundStreamView.PROMOTED_ID +

            // activity user (who commented, favorited etc. on contained following)
            ",Users." + TableColumns.Users.USERNAME + " as " + TableColumns.SoundStreamView.REPOSTER_USERNAME +
            ",Users." + TableColumns.Users.PERMALINK + " as " + TableColumns.SoundStreamView.REPOSTER_PERMALINK +
            ",Users." + TableColumns.Users.AVATAR_URL + " as " + TableColumns.SoundStreamView.REPOSTER_AVATAR_URL +

            // track+user data
            ",SoundView.*" +

            " FROM SoundStream" +

            // filter out duplicates
            " INNER JOIN (" +
            " SELECT _id, MAX(created_at) FROM SoundStream GROUP BY sound_id, sound_type, promoted_id " +
            ") dupes ON SoundStream._id = dupes._id " +


            " LEFT JOIN Users ON(" +
            "   SoundStream." + TableColumns.SoundStream.REPOSTER_ID + " = " + "Users." + TableColumns.Users._ID + ")" +
            " INNER JOIN SoundView ON(" +
            "   SoundStream." + TableColumns.SoundStream.SOUND_ID + " = " + "SoundView." + TableColumns.SoundView._ID + " AND " +
            "   SoundStream." + TableColumns.SoundStream.SOUND_TYPE + " = " + "SoundView." + TableColumns.SoundView._TYPE + ")" +

            " ORDER BY SoundStream." + TableColumns.SoundStreamView.CREATED_AT + " DESC";

    /**
     * A view which combines activity data + tracks/users/comments
     */
    static final String DATABASE_CREATE_ACTIVITY_VIEW = "AS SELECT " +
            "Activities." + TableColumns.Activities._ID + " as " + TableColumns.ActivityView._ID +
            ",Activities." + TableColumns.Activities.UUID + " as " + TableColumns.ActivityView.UUID +
            ",Activities." + TableColumns.Activities.TYPE + " as " + TableColumns.ActivityView.TYPE +
            ",Activities." + TableColumns.Activities.TAGS + " as " + TableColumns.ActivityView.TAGS +
            ",Activities." + TableColumns.Activities.CREATED_AT + " as " + TableColumns.ActivityView.CREATED_AT +
            ",Activities." + TableColumns.Activities.COMMENT_ID + " as " + TableColumns.ActivityView.COMMENT_ID +
            ",Activities." + TableColumns.Activities.SOUND_ID + " as " + TableColumns.ActivityView.SOUND_ID +
            ",Activities." + TableColumns.Activities.SOUND_TYPE + " as " + TableColumns.ActivityView.SOUND_TYPE +
            ",Activities." + TableColumns.Activities.USER_ID + " as " + TableColumns.ActivityView.USER_ID +
            ",Activities." + TableColumns.Activities.CONTENT_ID + " as " + TableColumns.ActivityView.CONTENT_ID +
            ",Activities." + TableColumns.Activities.SHARING_NOTE_TEXT + " as " + TableColumns.ActivityView.SHARING_NOTE_TEXT +
            ",Activities." + TableColumns.Activities.SHARING_NOTE_CREATED_AT + " as " + TableColumns.ActivityView.SHARING_NOTE_CREATED_AT +

            // activity user (who commented, favorited etc. on contained following)
            ",Users." + TableColumns.Users.USERNAME + " as " + TableColumns.ActivityView.USER_USERNAME +
            ",Users." + TableColumns.Users.PERMALINK + " as " + TableColumns.ActivityView.USER_PERMALINK +
            ",Users." + TableColumns.Users.AVATAR_URL + " as " + TableColumns.ActivityView.USER_AVATAR_URL +

            // track+user data
            ",SoundView.*" +

            // comment data (only for type=comment)
            ",Comments." + TableColumns.Comments.BODY + " as " + TableColumns.ActivityView.COMMENT_BODY +
            ",Comments." + TableColumns.Comments.CREATED_AT + " as " + TableColumns.ActivityView.COMMENT_CREATED_AT +
            ",Comments." + TableColumns.Comments.TIMESTAMP + " as " + TableColumns.ActivityView.COMMENT_TIMESTAMP +
            " FROM Activities" +
            " LEFT JOIN Users ON(" +
            "   Activities." + TableColumns.Activities.USER_ID + " = " + "Users." + TableColumns.Users._ID + ")" +
            " LEFT JOIN SoundView ON(" +
            "   Activities." + TableColumns.Activities.SOUND_ID + " = " + "SoundView." + TableColumns.SoundView._ID + " AND " +
            "   Activities." + TableColumns.Activities.SOUND_TYPE + " = " + "SoundView." + TableColumns.SoundView._TYPE + ")" +
            " LEFT JOIN Comments ON(" +
            "   Activities." + TableColumns.Activities.COMMENT_ID + " = " + "Comments." + TableColumns.Comments._ID + ")" +
            // filter out duplicates
            " LEFT JOIN Activities track_dup ON(" +
            "   track_dup.sound_id = Activities.sound_id AND " +
            "   track_dup.type = 'track-sharing' AND Activities.type = 'track'" +
            ")" +
            " LEFT JOIN Activities set_dup ON(" +
            "   set_dup.sound_id = Activities.sound_id AND " +
            "   set_dup.type = 'playlist-sharing' AND Activities.type = 'playlist'" +
            ")" +
            " WHERE track_dup._id IS NULL AND set_dup._id IS NULL AND" +
            // filter out activities with playables marked for removal or removed
            " (Activities." + TableColumns.ActivityView.TYPE + " == '" + ActivityKind.USER_FOLLOW + "' OR SoundView." + TableColumns.SoundView._ID + " IS NOT NULL)" +
            " ORDER BY " + TableColumns.ActivityView.CREATED_AT + " DESC";
}
