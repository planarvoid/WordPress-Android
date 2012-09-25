package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

import android.database.Cursor;
import android.os.Parcel;

/*
    {
      "type": "favoriting",
      "tags": "own",
      "created_at": "2011/07/22 17:12:46 +0000",
      "origin": {
        "track": {
         ... MINI TRACK ...
        },
        "user": {
          "permalink": "changmangoo",
          "id": 4462218,
          "permalink_url": "http://soundcloud.com/changmangoo",
          "uri": "https://api.soundcloud.com/users/4462218",
          "kind": "user",
          "avatar_url": "http://i1.sndcdn.com/avatars-000003571226-xdhfqb-large.jpg?0dfc9e6",
          "username": "changmangoo"
        }
      }
    }
 */

@JsonIgnoreProperties(ignoreUnknown=true)
public class Favoriting implements Origin {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public User user;

    public Favoriting() {
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
    public void setCachedTrack(Track track) {
        this.track = track;
    }

    @Override
    public void setCachedUser(User user) {
        this.user = user;
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
