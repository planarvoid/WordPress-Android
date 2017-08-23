package com.soundcloud.android.storage.schemas;

import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;

public class ActivityView {

    /**
     * A view which combines activity data + tracks/users/comments
     */
    public static final String DATABASE_CREATE_ACTIVITY_VIEW_VERSION_0_TO_115 = "AS SELECT " +
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

    /**
     * The most recent version of ActivityView
     *
     * A view which combines activity data + tracks/users/comments
     * And contains information about whether the user has a pro account
     * As introduced in DatabaseManager.upgradeTo116()
     */
    public static final String DATABASE_CREATE_ACTIVITY_VIEW_VERSION_116_AND_ABOVE = "AS SELECT " +
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
            "," + Tables.Users.IS_PRO + " as " + TableColumns.ActivityView.USER_IS_PRO +

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
