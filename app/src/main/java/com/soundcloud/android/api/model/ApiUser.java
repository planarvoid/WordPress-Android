package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;

import android.os.Parcel;

public class ApiUser extends ScModel implements PropertySetSource {

    private String country;
    private int followersCount;
    private String username;
    private String avatarUrl;

    public ApiUser() { /* for Deserialization */ }

    public ApiUser(String urn) {
        super(urn);
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    @JsonProperty("followers_count")
    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
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

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                UserProperty.URN.bind(getUrn()),
                UserProperty.USERNAME.bind(getUsername()),
                UserProperty.COUNTRY.bind(getCountry()),
                UserProperty.FOLLOWERS_COUNT.bind(getFollowersCount())
        );
    }
}
