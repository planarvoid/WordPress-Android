package com.soundcloud.android.model.Activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;

public class TrackSharingActivity extends Activity {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public SharingNote sharing_note;

    public TrackSharingActivity() {}

    public TrackSharingActivity(Cursor cursor) {
        super(cursor);
    }

    @Override
    public Type getType() {
        return Type.TRACK_SHARING;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public User getUser() {
        return track.user;
    }

    @JsonIgnore @Override
    public void setCachedTrack(Track track) {
            this.track = track;
            this.sharing_note = track.sharing_note;
        }

    @JsonIgnore @Override
    public void setCachedUser(User user) {
    }

    /*
    @Override  @JsonIgnore
    public Track getTrack() {
        // need to set sharing note on track since the API doesn't return the track
        // with sharing_note attached if activity type = 'track_sharing'
        if (track.sharing_note == null && sharing_note != null) {
            track.sharing_note = sharing_note;
        }
        return track;
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackSharingActivity that = (TrackSharingActivity) o;

        return !(sharing_note != null ? !sharing_note.equals(that.sharing_note) : that.sharing_note != null)
            && !(track != null ? !track.equals(that.track) : that.track != null);
    }

    @Override
    public int hashCode() {
        int result = track != null ? track.hashCode() : 0;
        result = 31 * result + (sharing_note != null ? sharing_note.hashCode() : 0);
        return result;
    }

    public static class SharingNote {
        @JsonProperty @JsonView(Views.Mini.class) public String text;
        @JsonProperty @JsonView(Views.Mini.class) public Date created_at;


        public boolean isEmpty() {
            return TextUtils.isEmpty(text);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SharingNote that = (SharingNote) o;

            return !(created_at != null ? !created_at.equals(that.created_at) : that.created_at != null)
                    && !(text != null ? !text.equals(that.text) : that.text != null);

        }

        @Override
        public int hashCode() {
            int result = text != null ? text.hashCode() : 0;
            result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
            return result;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(track, 0);
        dest.writeString(sharing_note.text);
        dest.writeLong(sharing_note.created_at.getTime());
    }

    public static final Parcelable.Creator<TrackSharingActivity> CREATOR = new Parcelable.Creator<TrackSharingActivity>() {
        public TrackSharingActivity createFromParcel(Parcel in) {

            TrackSharingActivity ts = new TrackSharingActivity();
            ts.track = in.readParcelable(Track.class.getClassLoader());
            ts.sharing_note = new SharingNote();
            ts.sharing_note.text = in.readString();
            ts.sharing_note.created_at = new Date(in.readLong());
            return ts;
        }
        public TrackSharingActivity[] newArray(int size) {
            return new TrackSharingActivity[size];
        }
    };
}
