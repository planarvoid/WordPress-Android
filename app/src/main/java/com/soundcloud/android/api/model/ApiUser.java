package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.users.UserUrn;

import android.os.Parcel;

public class ApiUser extends ScModel {
    private String username;
    private String avatarUrl;

    public ApiUser() { /* for Deserialization */ }

    public ApiUser(String urn) {
        super(urn);
    }

    @Override
    public UserUrn getUrn() {
        return (UserUrn) super.getUrn();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonProperty("avatar_url")
    public void setAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
    }

    @Deprecated
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.username);
        dest.writeString(this.avatarUrl);
    }

    ApiUser(Parcel in) {
        super(in);
        this.username = in.readString();
        this.avatarUrl = in.readString();
    }

    public static Creator<ApiUser> CREATOR = new Creator<ApiUser>() {
        public ApiUser createFromParcel(Parcel source) {
            return new ApiUser(source);
        }

        public ApiUser[] newArray(int size) {
            return new ApiUser[size];
        }
    };
}
