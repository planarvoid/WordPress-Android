
package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.api.Wrapper;
import com.soundcloud.android.model.behavior.Identifiable;
import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
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
public abstract class Activity extends ScModel implements Parcelable,
        Refreshable,
        Comparable<Activity>,
        PlayableHolder,
        Identifiable,
        Persisted {

    @JsonProperty public String uuid;
    @JsonProperty public Date created_at;
    @JsonProperty public String tags;
    @JsonProperty public SharingNote sharing_note;

    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;
    // cache human readable elapsed time
    private String _elapsedTime;

    /** needed for Deserialization */
    public Activity() {
    }

    public Activity(Parcel in) {
        created_at = new Date(in.readLong());
        tags = in.readString();
        sharing_note = new SharingNote();
        sharing_note.text = in.readString();
        final long milliseconds = in.readLong();
        sharing_note.created_at = milliseconds == -1l ? null : new Date(milliseconds);
    }

    public Activity(Cursor c) {
        id = c.getLong(c.getColumnIndex(DBHelper.ActivityView._ID));
        uuid = c.getString(c.getColumnIndex(DBHelper.ActivityView.UUID));
        tags = c.getString(c.getColumnIndex(DBHelper.ActivityView.TAGS));
        created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.CREATED_AT)));

        sharing_note = new SharingNote();
        sharing_note.created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.SHARING_NOTE_CREATED_AT)));
        sharing_note.text = c.getString(c.getColumnIndex(DBHelper.ActivityView.SHARING_NOTE_TEXT));
    }

    @Override
    public long getListItemId() {
        return toUUID().hashCode();
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

    public String getDateString() {
        return created_at == null ? null :
                Wrapper.CloudDateFormat.formatDate(created_at.getTime());
    }

    public UUID toUUID() {
        if (!TextUtils.isEmpty(uuid)){
            return UUID.fromString(uuid);
        } else if (created_at != null) {
            // snippet from http://wiki.apache.org/cassandra/FAQ#working_with_timeuuid_in_java
            final long origTime = created_at.getTime();
            final long time = origTime * 10000 + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
            final long timeLow = time &       0xffffffffL;
            final long timeMid = time &   0xffff00000000L;
            final long timeHi  = time & 0xfff000000000000L;
            final long upperLong = (timeLow << 32) | (timeMid >> 16) | (1 << 12) | (timeHi >> 48) ;
            return new UUID(upperLong, 0xC000000000000000L);
        } else {
            return null;
        }
    }

    public String toGUID() {
        if (!TextUtils.isEmpty(uuid)){
            return uuid;
        } else {
            UUID gen = toUUID();
            return gen == null ? null :gen.toString();
        }
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Activities.UUID, uuid);
        cv.put(DBHelper.Activities.TAGS, tags);
        cv.put(DBHelper.Activities.TYPE, getType().type);
        if (sharing_note != null){
            cv.put(DBHelper.Activities.SHARING_NOTE_TEXT, sharing_note.text);
            cv.put(DBHelper.Activities.SHARING_NOTE_CREATED_AT, sharing_note.created_at.getTime());
        }

        if (created_at != null) {
            cv.put(DBHelper.Activities.CREATED_AT, created_at.getTime());
        }

        if (getUser() != null) cv.put(DBHelper.Activities.USER_ID, getUser().id);

        if (getPlayable() != null){
            cv.put(DBHelper.Activities.SOUND_ID, getPlayable().id);
            cv.put(DBHelper.Activities.SOUND_TYPE, getType().isPlaylistActivity() ? Playable.DB_TYPE_PLAYLIST : Playable.DB_TYPE_TRACK);
        }
        return cv;
    }

    @Override
    public void putFullContentValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Activity)) return false;

        Activity activity = (Activity) o;

        if (created_at != null ? !created_at.equals(activity.created_at) : activity.created_at != null) return false;
        if (sharing_note != null ? !sharing_note.equals(activity.sharing_note) : activity.sharing_note != null)
            return false;
        if (tags != null ? !tags.equals(activity.tags) : activity.tags != null) return false;
        if (uuid != null ? !uuid.equals(activity.uuid) : activity.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (sharing_note != null ? sharing_note.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Activity activity) {
        return activity.created_at.compareTo(created_at);
    }

    @Override
    public boolean isStale() {
        final ScResource r = getRefreshableResource();
        if (equals(r)) {
            // no lookups possible on activity, though they would solve deletion problems
            throw new IllegalArgumentException("Do not return the activity itself as the refreshable object");
        } else {
            return r instanceof Refreshable && ((Refreshable) r).isStale();
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(created_at.getTime());
        out.writeString(tags == null ? "" : tags);
        out.writeString(sharing_note == null ? "" : sharing_note.text);
        out.writeLong(sharing_note == null ? -1l : sharing_note.created_at.getTime());
    }

    public abstract Type        getType();
    public abstract User        getUser();
    @Deprecated
    public abstract void        cacheDependencies();

    public List<ScResource> getDependentModels() {
        List<ScResource> models = new ArrayList<ScResource>();
        final User user = getUser();
        if (user != null)  models.add(user);

        final Playable playable = getPlayable();
        if (playable != null)  {
            models.add(playable);
            if (playable.user != null){
                models.add(playable.user);
            }
        }
        return models;
    }

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

        public static final EnumSet<Type> PLAYLIST_TYPES = EnumSet.of(PLAYLIST, PLAYLIST_LIKE, PLAYLIST_REPOST, PLAYLIST_SHARING);

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

        public static Type fromString(@NotNull String type) {
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

        public boolean isPlaylistActivity() {
            return PLAYLIST_TYPES.contains(this);
        }
    }

    public static String getDbPlaylistTypesForQuery() {
        String types = "";
        int i = 0;
        for (Type t : Activity.Type.PLAYLIST_TYPES) {
            types += "'" + t.type + "'";
            if (i < Activity.Type.PLAYLIST_TYPES.size() - 1) {
                types += ",";
            }
            i++;
        }
        return types;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public Uri toUri() {
        return null;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.ME_ALL_ACTIVITIES.uri;
    }
}
