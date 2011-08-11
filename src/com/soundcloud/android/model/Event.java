
package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends ModelBase {
    public Date created_at;
    public String type;
    public String tags;
    public String label;

    // locally used field
    public long user_id;
    // stored in db
    public long origin_id;
    /** @noinspection UnusedDeclaration*/
    public boolean exclusive;
    /** @noinspection UnusedDeclaration*/
    public String next_cursor;

    public Track track;
    public Comment comment;
    public User user;
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

    public User getUser(){
         if (type.contentEquals(Types.TRACK)) {
            return track.user;
        } else if (type.contentEquals(Types.TRACK_SHARING)) {
            return track.user;
        } else if (type.contentEquals(Types.COMMENT)) {
            return comment.user;
        } else if (type.contentEquals(Types.FAVORITING)) {
            return user;
        }
        return null;
    }

    public Track getTrack() {
        return type.contentEquals(Types.COMMENT) ? comment.track : track;
    }

    @Override
    public void resolve(SoundCloudApplication application) {
        if (track != null) {
            track.updateUserPlayedFromDb(application.getContentResolver(), application.getLoggedInUser());
        }

        if (type.contentEquals(Types.COMMENT)) {
            if (comment.track.user == null){
                 comment.track.user = application.getLoggedInUser();
            }
         } else if (type.contentEquals(Types.FAVORITING)) {
            if (track.user == null){
                track.user = application.getLoggedInUser();
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
        if (mElapsedTime == null){
            mElapsedTime = CloudUtils.getTimeElapsed(c.getResources(),created_at.getTime());
        }

        return mElapsedTime;
    }

    public interface Types {
        String TRACK = "track";
        String TRACK_SHARING = "track-sharing";
        String COMMENT = "comment";
        String FAVORITING = "favoriting";
    }

    @Override
    public String toString() {
        return "Event{" +
                "type='" + type + '\'' +
                ", track=" + (track == null ? "" : track.title) +
                ", user=" + (user == null ? "" : user.username) +
                '}';
    }
}
