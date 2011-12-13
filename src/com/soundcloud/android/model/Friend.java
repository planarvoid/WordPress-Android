
package com.soundcloud.android.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Friend extends ScModel {
    @SuppressWarnings({"UnusedDeclaration"})
    public long[] connection_ids;
    public User user;

    @SuppressWarnings({"UnusedDeclaration"})
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
}
