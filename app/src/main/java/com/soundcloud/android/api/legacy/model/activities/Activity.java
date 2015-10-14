package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.SharingNote;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.android.api.legacy.model.behavior.Persisted;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.java.collections.PropertySet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
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
        @JsonSubTypes.Type(value = PlaylistLikeActivity.class, name = "playlist-like"),
        @JsonSubTypes.Type(value = PlaylistRepostActivity.class, name = "playlist-repost"),
        @JsonSubTypes.Type(value = TrackLikeActivity.class, name = "track-like"),
        @JsonSubTypes.Type(value = TrackRepostActivity.class, name = "track-repost"),
        @JsonSubTypes.Type(value = UserMentionActivity.class, name = "user-mention"),
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
    @JsonProperty public String tags;
    @JsonProperty public SharingNote sharing_note;

    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

    protected Date createdAt;

    /**
     * needed for Deserialization
     */
    public Activity() {
    }

    public Activity(Parcel in) {
        createdAt = new Date(in.readLong());
        tags = in.readString();
        sharing_note = new SharingNote();
        sharing_note.text = in.readString();
        final long milliseconds = in.readLong();
        sharing_note.created_at = milliseconds == -1l ? null : new Date(milliseconds);
    }

    public Activity(Cursor c) {
        setId(c.getLong(c.getColumnIndex(TableColumns.ActivityView._ID)));
        uuid = c.getString(c.getColumnIndex(TableColumns.ActivityView.UUID));
        tags = c.getString(c.getColumnIndex(TableColumns.ActivityView.TAGS));
        createdAt = new Date(c.getLong(c.getColumnIndex(TableColumns.ActivityView.CREATED_AT)));

        sharing_note = new SharingNote();
        sharing_note.created_at = new Date(c.getLong(c.getColumnIndex(TableColumns.ActivityView.SHARING_NOTE_CREATED_AT)));
        sharing_note.text = c.getString(c.getColumnIndex(TableColumns.ActivityView.SHARING_NOTE_TEXT));
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Date getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setCreatedAt(Date date) {
        this.createdAt = date;
    }

    @Override
    public long getListItemId() {
        return toUUID().hashCode();
    }

    public UUID toUUID() {
        if (!TextUtils.isEmpty(uuid)) {
            return UUID.fromString(uuid);
        } else if (createdAt != null) {
            // snippet from http://wiki.apache.org/cassandra/FAQ#working_with_timeuuid_in_java
            final long origTime = createdAt.getTime();
            final long time = origTime * 10000 + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
            final long timeLow = time & 0xffffffffL;
            final long timeMid = time & 0xffff00000000L;
            final long timeHi = time & 0xfff000000000000L;
            final long upperLong = (timeLow << 32) | (timeMid >> 16) | (1 << 12) | (timeHi >> 48);
            return new UUID(upperLong, 0xC000000000000000L);
        } else {
            return null;
        }
    }

    public String toGUID() {
        if (!TextUtils.isEmpty(uuid)) {
            return uuid;
        } else {
            UUID gen = toUUID();
            return gen == null ? null : gen.toString();
        }
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(TableColumns.Activities.UUID, uuid);
        cv.put(TableColumns.Activities.TAGS, tags);
        cv.put(TableColumns.Activities.TYPE, getType().type);
        if (sharing_note != null) {
            cv.put(TableColumns.Activities.SHARING_NOTE_TEXT, sharing_note.text);
            cv.put(TableColumns.Activities.SHARING_NOTE_CREATED_AT, sharing_note.created_at.getTime());
        }

        if (createdAt != null) {
            cv.put(TableColumns.Activities.CREATED_AT, createdAt.getTime());
        }

        if (getUser() != null) {
            cv.put(TableColumns.Activities.USER_ID, getUser().getId());
        }

        if (getPlayable() != null) {
            cv.put(TableColumns.Activities.SOUND_ID, getPlayable().getId());
            cv.put(TableColumns.Activities.SOUND_TYPE, getType().isPlaylistActivity() ? Playable.DB_TYPE_PLAYLIST : Playable.DB_TYPE_TRACK);
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof Activity)) {
            return false;
        }

        Activity activity = (Activity) o;

        if (createdAt != null ? !createdAt.equals(activity.createdAt) : activity.createdAt != null) {
            return false;
        }
        if (tags != null ? !tags.equals(activity.tags) : activity.tags != null) {
            return false;
        }
        if (uuid != null ? !uuid.equals(activity.uuid) : activity.uuid != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Activity activity) {
        return activity.createdAt.compareTo(createdAt);
    }

    @Override
    public boolean isStale() {
        final Refreshable r = getRefreshableResource();
        if (equals(r)) {
            // no lookups possible on activity, though they would solve deletion problems
            throw new IllegalArgumentException("Do not return the activity itself as the refreshable object");
        } else {
            return r != null && r.isStale();
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(createdAt.getTime());
        out.writeString(tags == null ? "" : tags);
        out.writeString(sharing_note == null ? "" : sharing_note.text);
        out.writeLong(sharing_note == null ? -1l : sharing_note.created_at.getTime());
    }

    public abstract Type getType();

    public abstract PublicApiUser getUser();

    @Deprecated
    public abstract void cacheDependencies();

    public List<PublicApiResource> getDependentModels() {
        List<PublicApiResource> models = new ArrayList<>();
        final PublicApiUser user = getUser();
        if (user != null) {
            models.add(user);
        }

        final Playable playable = getPlayable();
        if (playable != null) {
            models.add(playable);
            if (playable.user != null) {
                models.add(playable.user);
            }
        }
        return models;
    }

    // todo : row types, upgrade DB
    public enum Type {
        TRACK_LIKE("track-like", TrackLikeActivity.class),
        TRACK_REPOST("track-repost", TrackRepostActivity.class),
        PLAYLIST_LIKE("playlist-like", PlaylistLikeActivity.class),
        PLAYLIST_REPOST("playlist-repost", PlaylistRepostActivity.class),
        COMMENT("comment", CommentActivity.class),
        USER_MENTION("user-mention", UserMentionActivity.class),
        AFFILIATION("affiliation", AffiliationActivity.class);

        public static final EnumSet<Type> PLAYLIST_TYPES = EnumSet.of(PLAYLIST_LIKE, PLAYLIST_REPOST);

        Type(String type, Class<? extends Activity> activityClass) {
            this.type = type;
            this.activityClass = activityClass;
        }

        public final String type;
        public final Class<? extends Activity> activityClass;

        public Activity fromCursor(Cursor cursor) {
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

    public PropertySet toPropertySet() {
        return PropertySet.create(5)
                .put(ActivityProperty.DATE, createdAt)
                .put(ActivityProperty.USER_NAME, getUser().getUsername())
                .put(ActivityProperty.USER_URN, getUser().getUrn());
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
