package com.soundcloud.android.model;

import android.os.Parcel;

public class MiniUser extends ScModel {
    private String mUsername;

    public MiniUser() { /* for Deserialization */ }

    public MiniUser(String urn) {
        super(urn);
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mUsername);
    }

    MiniUser(Parcel in) {
        super(in);
        this.mUsername = in.readString();
    }

    public static Creator<MiniUser> CREATOR = new Creator<MiniUser>() {
        public MiniUser createFromParcel(Parcel source) {
            return new MiniUser(source);
        }

        public MiniUser[] newArray(int size) {
            return new MiniUser[size];
        }
    };
}
