
package com.soundcloud.android.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;

@JsonIgnoreProperties(ignoreUnknown = true)
// TODO not used, merge with user later
public class Friend extends ScModel implements Resource, Origin {
    public long[] connection_ids;
    public User user;

    public Friend() {
    }

    public Friend(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Friend> CREATOR = new Parcelable.Creator<Friend>() {
        public Friend createFromParcel(Parcel in) {
            return new Friend(in);
        }

        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };

     @Override
    public long getLastUpdated(){
        return user.last_updated;
    }

    @Override
    public long getStaleTime() {
        return user.getStaleTime();
    }

    @Override
    public long getResourceId() {
        return user.id;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }
}
