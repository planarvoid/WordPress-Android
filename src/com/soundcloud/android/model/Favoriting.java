package com.soundcloud.android.model;

import com.soundcloud.android.json.Views;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import android.database.Cursor;
import android.os.Parcel;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Favoriting implements Origin {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public User user;

    public Favoriting() {
    }

    public Favoriting(Cursor c) {
        user = User.fromActivityView(c);
        track = new Track(c);
    }

    @Override @JsonIgnore
    public Track getTrack() {
        return track;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Favoriting that = (Favoriting) o;
        return !(track != null ? !track.equals(that.track) : that.track != null)
                && !(user != null ? !user.equals(that.user) : that.user != null);
    }

    @Override
    public int hashCode() {
        int result = track != null ? track.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(track, 0);
        dest.writeParcelable(user, 0);
    }

    public static final Creator<Favoriting> CREATOR = new Creator<Favoriting>() {
        public Favoriting createFromParcel(Parcel in) {
            Favoriting t = new Favoriting();
            t.track = in.readParcelable(Track.class.getClassLoader());
            t.user = in.readParcelable(User.class.getClassLoader());
            return t;
        }
        public Favoriting[] newArray(int size) {
            return new Favoriting[size];
        }
    };
}
