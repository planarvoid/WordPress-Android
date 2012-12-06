package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SharingNote;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class TrackSharingActivity extends Activity implements Playable {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;

    public TrackSharingActivity() {
        super();
    }

    public TrackSharingActivity(Cursor cursor) {
        super(cursor);
    }

    public TrackSharingActivity(Parcel in) {
        super(in);
        track = in.readParcelable(Track.class.getClassLoader());
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

    @Override
    public Playlist getPlaylist() {
        return null;
    }

    @JsonIgnore
    @Override
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @JsonIgnore @Override
    public void setCachedUser(User user) {
    }

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest,flags);
        dest.writeParcelable(track, 0);
    }

    public static final Parcelable.Creator<TrackSharingActivity> CREATOR = new Parcelable.Creator<TrackSharingActivity>() {
        public TrackSharingActivity createFromParcel(Parcel in) {
            return new TrackSharingActivity(in);
        }
        public TrackSharingActivity[] newArray(int size) {
            return new TrackSharingActivity[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackSharingActivity)) return false;
        if (!super.equals(o)) return false;

        TrackSharingActivity that = (TrackSharingActivity) o;

        if (track != null ? !track.equals(that.track) : that.track != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (track != null ? track.hashCode() : 0);
        return result;
    }
}
