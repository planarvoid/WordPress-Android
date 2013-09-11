package com.soundcloud.android.model;

import com.soundcloud.android.utils.images.ImageSize;

import android.os.Parcel;

public class UserSummary extends ScModel {
    private String mUsername;

    public UserSummary() { /* for Deserialization */ }

    public UserSummary(String urn) {
        super(urn);
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        this.mUsername = username;
    }

    public String getAvatar(ImageSize imageSize) {
        return mURN.imageUri(imageSize).toString();
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

    UserSummary(Parcel in) {
        super(in);
        this.mUsername = in.readString();
    }

    public static Creator<UserSummary> CREATOR = new Creator<UserSummary>() {
        public UserSummary createFromParcel(Parcel source) {
            return new UserSummary(source);
        }

        public UserSummary[] newArray(int size) {
            return new UserSummary[size];
        }
    };
}
