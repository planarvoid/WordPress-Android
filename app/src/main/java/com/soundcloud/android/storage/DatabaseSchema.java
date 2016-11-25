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

    static final String DATABASE_CREATE_WAVEFORMS = "(" +
            "track_id INTEGER, " +
            "max_amplitude INTEGER, " +
            "samples TEXT, " +
            "created_at INTEGER," +
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

    static final String DATABASE_CREATE_SOUND_VIEW = "AS SELECT " +
            Tables.Sounds._ID.qualifiedName() + " as " + TableColumns.SoundView._ID +
            "," + Tables.Sounds._TYPE.qualifiedName() + " as " + TableColumns.SoundView._TYPE +
            "," + Tables.Sounds.LAST_UPDATED.qualifiedName() + " as " + TableColumns.SoundView.LAST_UPDATED +
            "," + Tables.Sounds.PERMALINK.qualifiedName() + " as " + TableColumns.SoundView.PERMALINK +
            "," + Tables.Sounds.CREATED_AT.qualifiedName() + " as " + TableColumns.SoundView.CREATED_AT +
            "," + Tables.Sounds.DURATION.qualifiedName() + " as " + TableColumns.SoundView.DURATION +
            "," + Tables.Sounds.SNIPPET_DURATION.qualifiedName() + " as " + TableColumns.SoundView.SNIPPET_DURATION +
            "," + Tables.Sounds.FULL_DURATION.qualifiedName() + " as " + TableColumns.SoundView.FULL_DURATION +
            "," + Tables.Sounds.ORIGINAL_CONTENT_SIZE.qualifiedName() + " as " + TableColumns.SoundView.ORIGINAL_CONTENT_SIZE +
            "," + Tables.Sounds.STATE.qualifiedName() + " as " + TableColumns.SoundView.STATE +
            "," + Tables.Sounds.GENRE.qualifiedName() + " as " + TableColumns.SoundView.GENRE +
            "," + Tables.Sounds.TAG_LIST.qualifiedName() + " as " + TableColumns.SoundView.TAG_LIST +
            "," + Tables.Sounds.TRACK_TYPE.qualifiedName() + " as " + TableColumns.SoundView.TRACK_TYPE +
            "," + Tables.Sounds.TITLE.qualifiedName() + " as " + TableColumns.SoundView.TITLE +
            "," + Tables.Sounds.PERMALINK_URL.qualifiedName() + " as " + TableColumns.SoundView.PERMALINK_URL +
            "," + Tables.Sounds.ARTWORK_URL.qualifiedName() + " as " + TableColumns.SoundView.ARTWORK_URL +
            "," + Tables.Sounds.WAVEFORM_URL.qualifiedName() + " as " + TableColumns.SoundView.WAVEFORM_URL +
            "," + Tables.Sounds.DOWNLOADABLE.qualifiedName() + " as " + TableColumns.SoundView.DOWNLOADABLE +
            "," + Tables.Sounds.DOWNLOAD_URL.qualifiedName() + " as " + TableColumns.SoundView.DOWNLOAD_URL +
            "," + Tables.Sounds.STREAM_URL.qualifiedName() + " as " + TableColumns.SoundView.STREAM_URL +
            "," + Tables.Sounds.STREAMABLE.qualifiedName() + " as " + TableColumns.SoundView.STREAMABLE +
            "," + Tables.Sounds.COMMENTABLE.qualifiedName() + " as " + TableColumns.SoundView.COMMENTABLE +
            "," + Tables.Sounds.SHARING.qualifiedName() + " as " + TableColumns.SoundView.SHARING +
            "," + Tables.Sounds.LICENSE.qualifiedName() + " as " + TableColumns.SoundView.LICENSE +
            "," + Tables.Sounds.PURCHASE_URL.qualifiedName() + " as " + TableColumns.SoundView.PURCHASE_URL +
            "," + Tables.Sounds.PLAYBACK_COUNT.qualifiedName() + " as " + TableColumns.SoundView.PLAYBACK_COUNT +
            "," + Tables.Sounds.DOWNLOAD_COUNT.qualifiedName() + " as " + TableColumns.SoundView.DOWNLOAD_COUNT +
            "," + Tables.Sounds.COMMENT_COUNT.qualifiedName() + " as " + TableColumns.SoundView.COMMENT_COUNT +
            "," + Tables.Sounds.LIKES_COUNT.qualifiedName() + " as " + TableColumns.SoundView.LIKES_COUNT +
            "," + Tables.Sounds.REPOSTS_COUNT.qualifiedName() + " as " + TableColumns.SoundView.REPOSTS_COUNT +
            "," + Tables.Sounds.SHARED_TO_COUNT.qualifiedName() + " as " + TableColumns.SoundView.SHARED_TO_COUNT +
            "," + Tables.Sounds.TRACKS_URI.qualifiedName() + " as " + TableColumns.SoundView.TRACKS_URI +
            "," + Tables.Sounds.TRACK_COUNT.qualifiedName() + " as " + TableColumns.SoundView.TRACK_COUNT +
            "," + Tables.Sounds.DESCRIPTION.qualifiedName() + " as " + TableColumns.SoundView.DESCRIPTION +
            "," + Tables.Sounds.IS_ALBUM.qualifiedName() + " as " + TableColumns.SoundView.IS_ALBUM +
            "," + Tables.Sounds.SET_TYPE.qualifiedName() + " as " + TableColumns.SoundView.SET_TYPE +
            "," + Tables.Sounds.RELEASE_DATE.qualifiedName() + " as " + TableColumns.SoundView.RELEASE_DATE +
            "," + Tables.TrackPolicies.MONETIZABLE.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_MONETIZABLE +
            "," + Tables.TrackPolicies.BLOCKED.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_BLOCKED +
            "," + Tables.TrackPolicies.SNIPPED.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_SNIPPED +
            "," + Tables.TrackPolicies.POLICY.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_POLICY +
            "," + Tables.TrackPolicies.SYNCABLE.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_SYNCABLE +
            "," + Tables.TrackPolicies.SUB_MID_TIER.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_SUB_MID_TIER +
            "," + Tables.TrackPolicies.SUB_HIGH_TIER.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_SUB_HIGH_TIER +
            "," + Tables.TrackPolicies.MONETIZATION_MODEL.qualifiedName() + " as " + TableColumns.SoundView.POLICIES_MONETIZATION_MODEL +
            "," + Tables.Users._ID + " as " + TableColumns.SoundView.USER_ID +
            "," + Tables.Users.USERNAME + " as " + TableColumns.SoundView.USERNAME +
            "," + Tables.Users.PERMALINK + " as " + TableColumns.SoundView.USER_PERMALINK +
            "," + Tables.Users.AVATAR_URL + " as " + TableColumns.SoundView.USER_AVATAR_URL +
            "," + Tables.TrackDownloads.DOWNLOADED_AT + " as " + TableColumns.SoundView.OFFLINE_DOWNLOADED_AT +
            "," + Tables.TrackDownloads.REMOVED_AT + " as " + TableColumns.SoundView.OFFLINE_REMOVED_AT +
            " FROM Sounds" +
            " LEFT JOIN Users ON( " + Tables.Sounds.USER_ID.qualifiedName() + " = " + Tables.Users._ID + ")" +
            " LEFT OUTER JOIN TrackDownloads " +
            "   ON (" + Tables.Sounds._ID.qualifiedName() + " = " + Tables.TrackDownloads._ID + " AND " +
            "   " + Tables.Sounds._TYPE.qualifiedName() + " = " + Tables.Sounds.TYPE_TRACK + ")" +
            " LEFT OUTER JOIN TrackPolicies ON(" +
            "   " + Tables.Sounds._ID.qualifiedName() + " = " + Tables.TrackPolicies.TRACK_ID.qualifiedName() + ")" +
            " WHERE " + Tables.Sounds.REMOVED_AT.qualifiedName() + " IS NULL" +
            " AND (" + Tables.Sounds._TYPE.qualifiedName() + " != " + Tables.Sounds.TYPE_TRACK +
            " OR " + Tables.TrackPolicies.TRACK_ID.qualifiedName() + " IS NOT NULL)";

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
            "," + Tables.Users.USERNAME + " as " + TableColumns.SoundStreamView.REPOSTER_USERNAME +
            "," + Tables.Users.PERMALINK + " as " + TableColumns.SoundStreamView.REPOSTER_PERMALINK +
            "," + Tables.Users.AVATAR_URL + " as " + TableColumns.SoundStreamView.REPOSTER_AVATAR_URL +

            // track+user data
            ",SoundView.*" +

            " FROM SoundStream" +

            // filter out duplicates
            " INNER JOIN (" +
            " SELECT _id, MAX(created_at) FROM SoundStream GROUP BY sound_id, sound_type, promoted_id " +
            ") dupes ON SoundStream._id = dupes._id " +


            " LEFT JOIN Users ON(" +
            "   SoundStream." + TableColumns.SoundStream.REPOSTER_ID + " = " + Tables.Users._ID + ")" +
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
            "," + Tables.Users.USERNAME + " as " + TableColumns.ActivityView.USER_USERNAME +
            "," + Tables.Users.PERMALINK + " as " + TableColumns.ActivityView.USER_PERMALINK +
            "," + Tables.Users.AVATAR_URL + " as " + TableColumns.ActivityView.USER_AVATAR_URL +

            // track+user data
            ",SoundView.*" +

            // comment data (only for type=comment)
            ",Comments." + TableColumns.Comments.BODY + " as " + TableColumns.ActivityView.COMMENT_BODY +
            ",Comments." + TableColumns.Comments.CREATED_AT + " as " + TableColumns.ActivityView.COMMENT_CREATED_AT +
            ",Comments." + TableColumns.Comments.TIMESTAMP + " as " + TableColumns.ActivityView.COMMENT_TIMESTAMP +
            " FROM Activities" +
            " LEFT JOIN Users ON(" +
            "   Activities." + TableColumns.Activities.USER_ID + " = " + Tables.Users._ID + ")" +
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
