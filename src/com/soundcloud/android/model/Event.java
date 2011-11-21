
package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DBHelper.Events;
import com.soundcloud.android.provider.DBHelper.Tables;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends ModelBase implements Origin {
    @JsonProperty public Date created_at;
    @JsonProperty public String type;
    @JsonProperty public String tags;

    public Origin origin;

    private CharSequence mElapsedTime;

    public Event() {
    }

    public Event(Parcel in) {
        readFromParcel(in);
    }

    public Event(Cursor cursor, boolean aliasesOnly) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (aliasesOnly && !key.contains(Tables.EVENTS + "_")) continue;
            if (key.contentEquals(aliasesOnly ? Events.ALIAS_ID : Events.ID)) {
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                try {
                    setFieldFromCursor(this,
                            this.getClass().getDeclaredField(aliasesOnly ? key.substring(7) : key),
                            cursor, key);
                } catch (SecurityException e) {
                    Log.e(TAG, "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "error", e);
                }
            }
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

    @Override
    public void resolve(SoundCloudApplication application) {
        if (getTrack() != null) {
            getTrack().updateUserPlayedFromDb(application.getContentResolver(), application.getLoggedInUser());
        }

        if (origin instanceof Comment) {
            Comment c = (Comment)origin;
            if (c.track.user == null) {
                 c.track.user = application.getLoggedInUser();
            }
        } else if (type.contentEquals(Types.FAVORITING)) {
            if (getTrack().user == null) {
                getTrack().user = application.getLoggedInUser();
            }
        }
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public CharSequence getElapsedTime(Context c) {
        if (mElapsedTime == null) {
            mElapsedTime = CloudUtils.getTimeElapsed(c.getResources(),created_at.getTime());
        }
        return mElapsedTime;
    }

    public Class<?> getView(Class<?> defaultView) {
        if (isTrack() || isTrackSharing()) {
            return Views.Full.class;
        } else {
            return defaultView;
        }
    }

    public interface Types {
        String TRACK = "track";
        String TRACK_SHARING = "track-sharing";
        String COMMENT = "comment";
        String FAVORITING = "favoriting";
        String PLAYLIST = "playlist";
    }

    public Class<? extends Origin> getOriginClass() {
        if (isTrack()) {
            return Track.class;
        } else if (isTrackSharing()) {
            return TrackSharing.class;
        } else if (isComment()) {
            return Comment.class;
        } else if (isFavoriting()) {
            return Favoriting.class;
        } else if (isPlaylist()) {
         return Playlist.class;
        } throw new IllegalStateException("unknown type:" +type);
    }

    /* package */ boolean isTrack() {
        return Types.TRACK.equals(type);
    }

    /* package */ boolean isTrackSharing() {
        return Types.TRACK_SHARING.equals(type);
    }

    /* package */ boolean isComment() {
        return Types.COMMENT.equals(type);
    }

    /* package */ boolean isFavoriting() {
        return Types.FAVORITING.equals(type);
    }

    /* package */ boolean isPlaylist() {
        return Types.PLAYLIST.equals(type);
    }

    @Override
    public String toString() {
        return "Event{" +
                "type='" + type + '\'' +
                ", track=" + (getTrack() == null ? "" : getTrack().title) +
                ", user="  + (getUser() == null ? "" : getUser().username) +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Event event = (Event) o;

        if (created_at != null ? !created_at.equals(event.created_at) : event.created_at != null) return false;
        if (origin != null ? !origin.equals(event.origin) : event.origin != null) return false;
        if (tags != null ? !tags.equals(event.tags) : event.tags != null) return false;
        if (type != null ? !type.equals(event.type) : event.type != null) return false;

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
}
