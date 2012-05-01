
package com.soundcloud.android.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;

@JsonIgnoreProperties(ignoreUnknown = true)
//TODO not used, merge with user later
@Deprecated
public class Friend extends ScModel implements Refreshable, Origin {
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
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public long getRefreshableId() {
        return user.id;
    }

    @Override
    public ScModel getRefreshableResource() {
        return user;
    }

    @Override
    public boolean isStale() {
        return user.isStale();
    }
}
