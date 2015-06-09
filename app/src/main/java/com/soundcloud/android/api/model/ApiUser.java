package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserProperty;
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
    private String description;
    private String myspaceName;
    private String website;
    private String websiteTitle;
    private String discogsName;

    public ApiUser() { /* for Deserialization */ }

    public ApiUser(String urn) {
        super(urn);
    }

    ApiUser(Parcel in) {
        super(in);
        this.username = in.readString();
        this.avatarUrl = in.readString();
    }

    public ApiUser(Urn urn) {
        super(urn);
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
        return Optional.fromNullable(description);
    }

    @Override
    public Optional<String> getWebsiteUrl() {
        return Optional.fromNullable(website);
    }

    @Override
    public Optional<String> getWebsiteName() {
        return Optional.fromNullable(websiteTitle);
    }

    @Override
    public Optional<String> getDiscogsName() {
        return Optional.fromNullable(discogsName);
    }

    @Override
    public Optional<String> getMyspaceName() {
        return Optional.fromNullable(myspaceName);
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWebsiteUrl(String website) {
        this.website = website;
    }

    public void setWebsiteTitle(String websiteTitle) {
        this.websiteTitle = websiteTitle;
    }

    public void setMyspaceName(String myspaceName) {
        this.myspaceName = myspaceName;
    }

    public void setDiscogsName(String discogsName) {
        this.discogsName = discogsName;
    }
}
