
package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")

@JsonSubTypes({
        @JsonSubTypes.Type(value = AffiliationActivity.class, name = "affiliation"),
        @JsonSubTypes.Type(value = PlaylistActivity.class, name = "playlist"),
        @JsonSubTypes.Type(value = PlaylistLikeActivity.class, name = "playlist-like"),
        @JsonSubTypes.Type(value = PlaylistRepostActivity.class, name = "playlist-repost"),
        @JsonSubTypes.Type(value = PlaylistSharingActivity.class, name = "playlist-sharing"),
        @JsonSubTypes.Type(value = TrackActivity.class, name = "track"),
        @JsonSubTypes.Type(value = TrackLikeActivity.class, name = "track-like"),
        @JsonSubTypes.Type(value = TrackRepostActivity.class, name = "track-repost"),
        @JsonSubTypes.Type(value = TrackSharingActivity.class, name = "track-sharing"),
        @JsonSubTypes.Type(value = CommentActivity.class, name = "comment")
})

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Activity extends ScModel implements Refreshable, Comparable<Activity> {
    @JsonProperty public String uuid;
    @JsonProperty public Date created_at;
    @JsonProperty public String tags;

    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
    // cache human readable elapsed time
    private String _elapsedTime;

    /** needed for Deserialization */
    public Activity() {
    }

    public Activity(Parcel in) {
        created_at = new Date(in.readLong());
        tags = in.readString();
    }

    public Activity(Cursor c) {
        id = c.getLong(c.getColumnIndex(DBHelper.ActivityView._ID));
        uuid = c.getString(c.getColumnIndex(DBHelper.ActivityView.UUID));
        tags = c.getString(c.getColumnIndex(DBHelper.ActivityView.TAGS));
        created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.CREATED_AT)));
    }

    public CharSequence getTimeSinceCreated(Context context) {
        if (_elapsedTime == null){
            refreshTimeSinceCreated(context);
        }
        return _elapsedTime;
    }

    public void refreshTimeSinceCreated(Context context) {
        _elapsedTime = ScTextUtils.getTimeElapsed(context.getResources(), created_at.getTime());
    }

    @Override
    public void resolve(Context context) {
        refreshTimeSinceCreated(context);
    }

    public String getDateString() {
        return created_at == null ? null :
                AndroidCloudAPI.CloudDateFormat.formatDate(created_at.getTime());
    }

    public UUID toUUID() {
        if (created_at == null) {
            return null;
        } else {
            // snippet from http://wiki.apache.org/cassandra/FAQ#working_with_timeuuid_in_java
            final long origTime = created_at.getTime();
            final long time = origTime * 10000 + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
            final long timeLow = time &       0xffffffffL;
            final long timeMid = time &   0xffff00000000L;
            final long timeHi  = time & 0xfff000000000000L;
            final long upperLong = (timeLow << 32) | (timeMid >> 16) | (1 << 12) | (timeHi >> 48) ;
            return new UUID(upperLong, 0xC000000000000000L);
        }
    }

    public String toGUID() {
        final UUID uuid = toUUID();
        return uuid != null ? toUUID().toString() : null;
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Activities.UUID, uuid);
        cv.put(DBHelper.Activities.TAGS, tags);
        cv.put(DBHelper.Activities.TYPE, getType().type);

        if (created_at != null) {
            cv.put(DBHelper.Activities.CREATED_AT, created_at.getTime());
        }

        if (getUser() != null) cv.put(DBHelper.Activities.USER_ID, getUser().id);
        if (getTrack() != null) cv.put(DBHelper.Activities.TRACK_ID, getTrack().id);

        return cv;
    }

    @Override @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        return activity.uuid == uuid;
    }

    @Override
    public int hashCode() {
        int result = 13;
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Activity activity) {
        return activity.created_at.compareTo(created_at);
    }

    @Override
    public long getRefreshableId() {
        return id;
    }

    @Override
    public ScResource getRefreshableResource() {
        return null;
    }

    @Override
    public boolean isStale() {
        return false;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(created_at.getTime());
        out.writeString(tags);
    }

    public abstract Type getType();

    public abstract Track getTrack();

    public abstract User getUser();

    public abstract Playlist getPlaylist();

    public abstract void setCachedTrack(Track track);

    public abstract void setCachedUser(User user);


    public List<ScResource> getDependentModels() {
        List<ScResource> models = new ArrayList<ScResource>();
        final User user = getUser();
        if (user != null)  models.add(user);

        final Track track = getTrack();
        if (track != null) {
            models.add(track);
            if (track.user != null && track.user != user){
                models.add(track.user);
            }
        }

        final Playlist playlist = getPlaylist();
        if (playlist != null) {
            models.add(playlist);
            if (playlist.user != null && playlist.user != user) {
                models.add(playlist.user);
            }
        }

        return models;
    }

//           TRACK("track", Track.class),
//           TRACK_SHARING("track-sharing", TrackSharing.class),
//           COMMENT("comment", Comment.class),
//           FAVORITING("favoriting", Favoriting.class),
//           PLAYLIST("playlist", Playlist.class);


    // todo : row types, upgrade DB
    public enum Type {
        TRACK("track", TrackActivity.class),
        TRACK_LIKE("track-like", TrackLikeActivity.class),
        TRACK_REPOST("track-repost", TrackRepostActivity.class),
        TRACK_SHARING("track-sharing", TrackSharingActivity.class),
        PLAYLIST("playlist", PlaylistActivity.class),
        PLAYLIST_LIKE("playlist-like", PlaylistLikeActivity.class),
        PLAYLIST_REPOST("playlist-repost", PlaylistRepostActivity.class),
        PLAYLIST_SHARING("playlist-sharing", PlaylistSharingActivity.class),
        COMMENT("comment", CommentActivity.class),
        AFFILIATION("affiliation", AffiliationActivity.class);

        Type(String type, Class<? extends Activity> activityClass) {
            this.type = type;
            this.activityClass = activityClass;
        }

        public final String type;
        public final Class<? extends Activity> activityClass;

        public Activity fromCursor(Cursor cursor){
            try {
                return activityClass.getConstructor(Cursor.class).newInstance(cursor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static Type fromString(String type) {
            for (Type t : values()) {
                if (t.type.equals(type)) {
                    return t;
                }
            }
            throw new IllegalStateException("unknown type:" + type);
        }

        @Override
        public String toString() {
            return type;
        }
    }
}
