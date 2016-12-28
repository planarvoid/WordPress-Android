package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserRecordHolder;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ApiUser implements ApiEntityHolder, UserRecord, UserRecordHolder, ApiSyncable {

    private Urn urn;
    @Nullable private String country;
    @Nullable private String city;
    private int followersCount;
    private String permalink;
    private String username;
    private Optional<String> avatarUrlTemplate = Optional.absent();
    private Optional<String> visualUrlTemplate = Optional.absent();
    private String description;
    private String myspaceName;
    private String website;
    private String websiteTitle;
    private String discogsName;
    private Urn artistStation;

    public ApiUser() { /* for Deserialization */ }

    ApiUser(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return avatarUrlTemplate;
    }

    @Override
    public Optional<String> getVisualUrlTemplate() {
        return visualUrlTemplate;
    }

    public void setUrn(Urn urn) {
        this.urn = urn;
    }

    public long getId() {
        return urn.getNumericId();
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Optional<String> getAvatarUrlTemplate() {
        return avatarUrlTemplate;
    }

    @JsonProperty("avatar_url_template")
    public void setAvatarUrlTemplate(String avatarUrlTemplate) {
        this.avatarUrlTemplate = Optional.fromNullable(avatarUrlTemplate);
    }

    @JsonProperty("visual_url_template")
    public void setVisualUrlTemplate(String visualUrlTemplate) {
        this.visualUrlTemplate = Optional.fromNullable(visualUrlTemplate);
    }

    @JsonProperty("station_urns")
    public void setStationUrns(List<Urn> stations) {
        for (Urn stationUrn : stations) {
            if (stationUrn.isArtistStation()) {
                this.artistStation = stationUrn;
            }
        }
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    @Nullable
    @Override
    public String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
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

    @Override
    public Optional<Urn> getArtistStationUrn() {
        return Optional.fromNullable(artistStation);
    }

    @JsonProperty("followers_count")
    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiUser that = (ApiUser) o;
        return MoreObjects.equal(urn, that.urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public UserRecord getUserRecord() {
        return this;
    }

    @Override
    public EntityStateChangedEvent toUpdateEvent() {
        return UserItem.from(this).toUpdateEvent();
    }
}
