
package com.soundcloud.android.model;

import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity extends ScModel implements Origin, Playable, Comparable<Activity> {
    @JsonProperty public Date created_at;
    @JsonProperty public Type type;
    @JsonProperty public String tags;

    static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

    /**
     * This maps to {@link Type}.
     */
    public Origin origin;

    // cache human readable elapsed time
    private String _elapsedTime;

    /** needed for {@link com.soundcloud.android.json.ActivityDeserializer} */
    public Activity() {
    }

    public Activity(Parcel in) {
        created_at = new Date(in.readLong());
        type = Type.fromString(in.readString());
        tags = in.readString();
        origin = in.readParcelable(Activity.class.getClassLoader());
    }

    public Activity(Cursor c) {
        id = c.getLong(c.getColumnIndex(DBHelper.ActivityView._ID));
        type =  Type.fromString(c.getString(c.getColumnIndex(DBHelper.ActivityView.TYPE)));
        tags = c.getString(c.getColumnIndex(DBHelper.ActivityView.TAGS));
        created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.CREATED_AT)));
        switch (type) {
            case TRACK:
                origin = SoundCloudApplication.TRACK_CACHE.fromActivityCursor(c);
                break;
            case TRACK_SHARING:
                origin = new TrackSharing(c);
                break;
            case COMMENT:
                origin = new Comment(c, true);
                break;
            case FAVORITING:
                origin = new Favoriting(c);
                break;
        }
    }

    public User getUser() {
        return origin == null ?  null : origin.getUser();
    }

    public Track getTrack() {
        return origin == null ? null : origin.getTrack();
    }

    public Comment getComment() {
        return origin instanceof Comment ? (Comment) origin : null;
    }

    public Favoriting getFavoriting() {
        return origin instanceof Favoriting ? (Favoriting) origin : null;
    }

    public TrackSharing getTrackSharing() {
        return origin instanceof TrackSharing ? (TrackSharing) origin : null;
    }

    @Override
    public void resolve(SoundCloudApplication application) {
        if (origin instanceof Comment) {
            Comment c = (Comment)origin;
            if (c.track.user == null) {
                 c.track.user = application.getLoggedInUser();
            }
        } else if (type == Type.FAVORITING) {
            if (getTrack().user == null) {
                getTrack().user = application.getLoggedInUser();
            }
        }
    }


    public CharSequence getElapsedTime(Context c) {
        if (_elapsedTime == null) {
            _elapsedTime = CloudUtils.getTimeElapsed(c.getResources(),created_at.getTime());
        }
        return _elapsedTime;
    }
    
    public String getDateString() {
        return created_at == null ? null :
                AndroidCloudAPI.CloudDateFormat.format(created_at.getTime());
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
    public String toString() {
        return "Activity{" +
                "type='" + type + '\'' +
                ", track=" + (getTrack() == null ? "" : getTrack().title) +
                ", user="  + (getUser() == null ? "" : getUser().username) +
                '}';
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.TYPE, type.toString());
        cv.put(DBHelper.Activities.TAGS, tags);

        if (created_at != null) {
            cv.put(DBHelper.Activities.CREATED_AT, created_at.getTime());
        }

        if (getUser() != null) {
            cv.put(DBHelper.Activities.USER_ID, getUser().id);
        }
        if (getTrack() != null) {
            cv.put(DBHelper.Activities.TRACK_ID, getTrack().id);
        }
        if (getComment() != null) {
            cv.put(DBHelper.Activities.COMMENT_ID, getComment().id);
        }
        return cv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Activity activity = (Activity) o;

        if (created_at != null ? !created_at.equals(activity.created_at) : activity.created_at != null) return false;
        if (origin != null ? !origin.equals(activity.origin) : activity.origin != null) return false;
        if (tags != null ? !tags.equals(activity.tags) : activity.tags != null) return false;
        if (type != null ? !type.equals(activity.type) : activity.type != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Activity activity) {
        return activity.created_at.compareTo(created_at);
    }

    public enum Type {
        TRACK("track", Track.class),
        TRACK_SHARING("track-sharing", TrackSharing.class),
        COMMENT("comment", Comment.class),
        FAVORITING("favoriting", Favoriting.class),
        PLAYLIST("playlist", Playlist.class);

        Type(String type, Class<? extends Origin> typeClass) {
            this.type = type;
            this.typeClass = typeClass;
        }
        public final String type;
        public final Class<? extends Origin> typeClass;

        public Class<?> getView(Class<?> defaultView) {
            if (this == TRACK || this == TRACK_SHARING) {
                return Views.Full.class;
            } else {
                return defaultView;
            }
        }
        public static Type fromString(String type) {
            for (Type t : values()) {
                if (t.type.equals(type)) {
                    return t;
                }
            }
            throw new IllegalStateException("unknown type:" +type);
        }
        @Override
        public String toString() {
            return type;
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(created_at.getTime());
        out.writeString(type.toString());
        out.writeString(tags);
        out.writeParcelable(origin, 0);
    }

    public static final Parcelable.Creator<Activity> CREATOR = new Parcelable.Creator<Activity>() {
        public Activity createFromParcel(Parcel in) {
            return new Activity(in);
        }

        public Activity[] newArray(int size) {
            return new Activity[size];
        }
    };


}
