package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import android.os.Parcel;

public class UserSummary extends ScModel {
    private String mUsername;
    private String mAvatarUrl;

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

    @JsonProperty("avatar_url")
    public void setAvatarUrl(String avatarUrl){
        mAvatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mUsername);
        dest.writeString(this.mAvatarUrl);
    }

    UserSummary(Parcel in) {
        super(in);
        this.mUsername = in.readString();
        this.mAvatarUrl = in.readString();
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
