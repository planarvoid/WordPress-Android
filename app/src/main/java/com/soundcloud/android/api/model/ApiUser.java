package com.soundcloud.android.api.model;

import static com.soundcloud.java.optional.Optional.fromNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserRecordHolder;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class ApiUser implements ApiEntityHolder, UserRecord, UserRecordHolder, ApiSyncable {

    private static final String DISCOGS_PATH = "http://www.discogs.com/artist/";
    private static final String DISCOGS_NETWORK = "discogs";
    private static final String MYSPACE_PATH = "http://www.myspace.com/";
    private static final String MYSPACE_NETWORK = "myspace";
    private static final String PERSONAL_NETWORK = "personal";

    private Urn urn;
    @Nullable private String country;
    @Nullable private String city;
    private int followersCount;
    private String permalink;
    private String username;
    private Optional<String> firstName = Optional.absent();
    private Optional<String> lastName = Optional.absent();
    private Optional<String> avatarUrlTemplate = Optional.absent();
    private Optional<String> visualUrlTemplate = Optional.absent();
    private String description;
    private String myspaceName;
    private String website;
    private String websiteTitle;
    private String discogsName;
    private Urn artistStation;
    private Optional<Date> createdAt = Optional.absent();

    // these are currently unused, but they come back in the representation, and are part of a future story
    private String countryCode;
    private int trackCount;
    private int followingsCount;
    private boolean isVerified;
    private boolean isPro;

    public ApiUser() { /* for Deserialization */ }

    ApiUser(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    public void setUrn(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return avatarUrlTemplate;
    }

    @Override
    public Optional<String> getVisualUrlTemplate() {
        return visualUrlTemplate;
    }

    public long getId() {
        return urn.getNumericId();
    }

    @Override
    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Optional<String> getAvatarUrlTemplate() {
        return avatarUrlTemplate;
    }

    @Override
    public Optional<String> getFirstName() {
        return firstName;
    }

    @JsonProperty("first_name")
    public void setFirstName(String name) {
        firstName = Optional.fromNullable(name);
    }

    @Override
    public Optional<String> getLastName() {
        return lastName;
    }

    @JsonProperty("last_name")
    public void setLastName(String name) {
        lastName = Optional.fromNullable(name);
    }

    @Override
    public Optional<Date> getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = Optional.fromNullable(createdAt);
    }

    @JsonProperty("avatar_url_template")
    public void setAvatarUrlTemplate(String avatarUrlTemplate) {
        this.avatarUrlTemplate = fromNullable(avatarUrlTemplate);
    }

    @JsonProperty("visual_url_template")
    public void setVisualUrlTemplate(String visualUrlTemplate) {
        this.visualUrlTemplate = fromNullable(visualUrlTemplate);
    }

    @JsonProperty("station_urns")
    public void setStationUrns(List<Urn> stations) {
        for (Urn stationUrn : stations) {
            if (stationUrn.isArtistStation()) {
                artistStation = stationUrn;
            }
        }
    }

    @Override
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

    @Override
    public int getFollowersCount() {
        return followersCount;
    }

    @JsonProperty("followers_count")
    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    @Override
    public Optional<String> getDescription() {
        return fromNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Optional<String> getWebsiteUrl() {
        return fromNullable(website);
    }

    public void setWebsiteUrl(String website) {
        this.website = website;
    }

    @Override
    public Optional<String> getWebsiteName() {
        return fromNullable(websiteTitle);
    }

    @Override
    public Optional<String> getDiscogsName() {
        return fromNullable(discogsName);
    }

    public void setDiscogsName(String discogsName) {
        this.discogsName = discogsName;
    }

    @Override
    public Optional<String> getMyspaceName() {
        return fromNullable(myspaceName);
    }

    public void setMyspaceName(String myspaceName) {
        this.myspaceName = myspaceName;
    }

    @Override
    public Optional<Urn> getArtistStationUrn() {
        return fromNullable(artistStation);
    }

    public void setWebsiteTitle(String websiteTitle) {
        this.websiteTitle = websiteTitle;
    }

    @JsonProperty("verified")
    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    @JsonProperty("is_pro")
    public void setIsPro(boolean isPro) {
        this.isPro = isPro;
    }


    @JsonProperty("track_count")
    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    @JsonProperty("followings_count")
    public void setFollowingsCount(int followingsCount) {
        this.followingsCount = followingsCount;
    }
    @JsonProperty("country_code")
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @JsonProperty("social_media_links")
    public void setApiSocialMediaLinks(ModelCollection<ApiSocialMediaLink> apiSocialMediaLinks) {
        // this converts new social links to the legacy format, and should be removed when we fully move
        // to the new format in https://soundcloud.atlassian.net/browse/CREATORS-2240
        for (ApiSocialMediaLink apiSocialMediaLink : apiSocialMediaLinks) {
            if (MYSPACE_NETWORK.equals(apiSocialMediaLink.network())) {
                setMyspaceName(apiSocialMediaLink.url().replace(MYSPACE_PATH, ""));
            } else if (DISCOGS_NETWORK.equals(apiSocialMediaLink.network())) {
                setDiscogsName(apiSocialMediaLink.url().replace(DISCOGS_PATH, ""));
            } else if (PERSONAL_NETWORK.equals(apiSocialMediaLink.network())) {
                setWebsiteUrl(apiSocialMediaLink.url());
                setWebsiteTitle(apiSocialMediaLink.title().orNull());
            }
        }
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

    public String getCountryCode() {
        return countryCode;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public int getFollowingsCount() {
        return followingsCount;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isPro() {
        return isPro;
    }
}
