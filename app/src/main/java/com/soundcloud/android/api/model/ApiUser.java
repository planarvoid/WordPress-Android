package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.os.Parcel;

public class ApiUser extends ScModel implements PropertySetSource, UserRecord {

    public static Creator<ApiUser> CREATOR = new Creator<ApiUser>() {
        public ApiUser createFromParcel(Parcel source) {
            return new ApiUser(source);
        }

        public ApiUser[] newArray(int size) {
            return new ApiUser[size];
        }
    };
    @Nullable private String country;
    private int followersCount;
    private String username;
    private String avatarUrl;

    public ApiUser() { /* for Deserialization */ }

    public ApiUser(String urn) {
        super(urn);
    }

    ApiUser(Parcel in) {
        super(in);
        this.username = in.readString();
        this.avatarUrl = in.readString();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Deprecated
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @JsonProperty("avatar_url")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.absent(); // not implement in api-mobi yet
    }

    @Override
    public Optional<String> getWebsiteUrl() {
        return Optional.absent(); // not implement in api-mobi yet
    }

    @Override
    public Optional<String> getWebsiteName() {
        return Optional.absent(); // not implement in api-mobi yet
    }

    @Override
    public Optional<String> getDiscogsName() {
        return Optional.absent(); // not implement in api-mobi yet
    }

    @Override
    public Optional<String> getMyspaceName() {
        return Optional.absent(); // not implement in api-mobi yet
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

    @Override
    public PropertySet toPropertySet() {
        final PropertySet bindings = PropertySet.from(
                UserProperty.URN.bind(urn),
                UserProperty.USERNAME.bind(username),
                UserProperty.FOLLOWERS_COUNT.bind(followersCount)
        );
        // this should be modeled with an Option type instead:
        // https://github.com/soundcloud/propeller/issues/32
        if (country != null) {
            bindings.put(UserProperty.COUNTRY, country);
        }
        return bindings;
    }
}
