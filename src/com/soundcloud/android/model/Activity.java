
package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity extends ScModel implements Origin, Playable {
    @JsonProperty public Date created_at;
    @JsonProperty public String type;
    @JsonProperty public String tags;

    public Origin origin;

    private CharSequence mElapsedTime;
    public String next_href;

    public Activity() {
    }

    public Activity(Parcel in) {
        readFromParcel(in);
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
        mElapsedTime = CloudUtils.getTimeElapsed(application.getResources(),created_at.getTime());
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

    public static final Parcelable.Creator<Activity> CREATOR = new Parcelable.Creator<Activity>() {
        public Activity createFromParcel(Parcel in) {
            return new Activity(in);
        }

        public Activity[] newArray(int size) {
            return new Activity[size];
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
        return "Activity{" +
                "type='" + type + '\'' +
                ", track=" + (getTrack() == null ? "" : getTrack().title) +
                ", user="  + (getUser() == null ? "" : getUser().username) +
                '}';
    }


    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Activities.TYPE, type);
        cv.put(DBHelper.Activities.TAGS, tags);
        
        if (getUser() != null) {
            cv.put(DBHelper.Activities.USER_ID, getUser().id);
        }
        if (getTrack() != null) {
            cv.put(DBHelper.Activities.TRACK_ID, getTrack().id);
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
}
