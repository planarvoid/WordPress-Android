package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicApiUser extends PublicApiResource implements UserHolder, UserRecord, ApiSyncable {
    public static final int TYPE = 0;
    public static final String EXTRA = "user";
    public static final Parcelable.Creator<PublicApiUser> CREATOR = new Parcelable.Creator<PublicApiUser>() {
        public PublicApiUser createFromParcel(Parcel in) {
            return new PublicApiUser(in);
        }

        public PublicApiUser[] newArray(int size) {
            return new PublicApiUser[size];
        }
    };

    @Nullable @JsonView(Views.Mini.class) public String username;
    @Nullable @JsonView(Views.Mini.class) public String uri;
    @Nullable @JsonView(Views.Mini.class) public String avatar_url;
    @Nullable @JsonView(Views.Mini.class) public String permalink;
    @Nullable @JsonView(Views.Mini.class) public String permalink_url;
    @Nullable public String full_name;
    @Nullable public String description;
    @Nullable public String plan;      // free|lite|solo|pro|pro plus
    @Nullable public String website;
    @Nullable public String website_title;
    @Nullable public String myspace_name;
    @Nullable public String discogs_name;
    // counts
    public int track_count = NOT_SET;
    public int followers_count = NOT_SET;
    public int followings_count = NOT_SET;
    public int private_tracks_count = NOT_SET;
    @JsonProperty("public_likes_count")
    public int public_likes_count = NOT_SET;
    // internal fields
    @Nullable private String city;
    @Nullable private String country;
    @Nullable private Boolean primary_email_confirmed;

    public PublicApiUser() {
    }

    public PublicApiUser(long id) {
        super(id);
    }

    public PublicApiUser(String urn) {
        super(urn);
    }

    public PublicApiUser(long id, String permalink) {
        super(id);
        this.username = permalink;
        this.permalink = permalink;
    }

    public PublicApiUser(Parcel in) {
        // TODO replace with generated file
        PublicApiUser model = this;
        Bundle bundle = in.readBundle(model.getClass().getClassLoader());
        model.username = bundle.getString("username");
        model.uri = bundle.getString("uri");
        model.avatar_url = bundle.getString("avatar_url");
        model.permalink = bundle.getString("permalink");
        model.permalink_url = bundle.getString("permalink_url");
        model.full_name = bundle.getString("full_name");
        model.description = bundle.getString("description");
        model.city = bundle.getString("city");
        model.country = bundle.getString("country");
        model.plan = bundle.getString("plan");
        model.website = bundle.getString("website");
        model.website_title = bundle.getString("website_title");
        model.myspace_name = bundle.getString("myspace_name");
        model.discogs_name = bundle.getString("discogs_name");
        model.track_count = bundle.getInt("track_count");
        model.followers_count = bundle.getInt("followers_count");
        model.followings_count = bundle.getInt("followings_count");
        model.public_likes_count = bundle.getInt("public_likes_count");
        model.private_tracks_count = bundle.getInt("private_tracks_count");
        model.setId(bundle.getLong("id"));
    }

    public PublicApiUser(UserRecord user) {
        setUrn(user.getUrn().toString());
        setUsername(user.getUsername());
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        urn = Urn.forUser(id);
    }

    @Override
    public String toString() {
        return "User[" +
                "id=" + getId() +
                ", username='" + username + '\'' +
                ", track_count='" + track_count + '\'' +
                ", discogs_name='" + discogs_name + '\'' +
                ", city='" + city + '\'' +
                ", uri='" + uri + '\'' +
                ", avatar_url='" + avatar_url + '\'' +
                ", website_title='" + website_title + '\'' +
                ", website='" + website + '\'' +
                ", description='" + description + '\'' +
                ", permalink='" + permalink + '\'' +
                ", permalink_url='" + permalink_url + '\'' +
                ", full_name='" + full_name + '\'' +
                ", followers_count='" + followers_count + '\'' +
                ", followings_count='" + followings_count + '\'' +
                ", public_likes_count='" + public_likes_count + '\'' +
                ", private_tracks_count='" + private_tracks_count + '\'' +
                ", myspace_name='" + myspace_name + '\'' +
                ", country='" + country + '\'' +
                ", plan='" + plan + '\'' +
                ", primary_email_confirmed=" + primary_email_confirmed +
                ']';
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public final void setUsername(@Nullable String username) {
        this.username = username;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatar_url;
    }

    public void setAvatarUrl(@Nullable String avatarUrl) {
        this.avatar_url = avatarUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPermalink() {
        return permalink;
    }

    public final void setPermalink(@Nullable String permalink) {
        this.permalink = permalink;
    }

    // setter for deserialization, we want it null if it doesn't exist and to keep it private
    @JsonProperty("primary_email_confirmed")
    public void setPrimaryEmailConfirmed(boolean val) {
        primary_email_confirmed = val;
    }

    @NotNull
    @Override
    public PublicApiUser getUser() {
        return this;
    }

    public final void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    @Nullable
    @Override
    public String getCity() {
        return city;
    }

    @Override
    public int getFollowersCount() {
        return followers_count;
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
        return Optional.fromNullable(website_title);
    }

    @Override
    public Optional<String> getDiscogsName() {
        return Optional.fromNullable(discogs_name);
    }

    @Override
    public Optional<String> getMyspaceName() {
        return Optional.fromNullable(myspace_name);
    }

    @Override
    public Optional<Urn> getArtistStationUrn() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getVisualUrlTemplate() {
        return Optional.absent();
    }

    public final void setCountry(@Nullable String country) {
        this.country = country;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // TODO replace with generated file
        Bundle bundle = new Bundle();
        PublicApiUser model = this;
        bundle.putString("username", model.username);
        bundle.putString("uri", model.uri);
        bundle.putString("avatar_url", model.avatar_url);
        bundle.putString("permalink", model.permalink);
        bundle.putString("permalink_url", model.permalink_url);
        bundle.putString("full_name", model.full_name);
        bundle.putString("description", model.description);
        bundle.putString("city", model.city);
        bundle.putString("country", model.country);
        bundle.putString("plan", model.plan);
        bundle.putString("website", model.website);
        bundle.putString("website_title", model.website_title);
        bundle.putString("myspace_name", model.myspace_name);
        bundle.putString("discogs_name", model.discogs_name);
        bundle.putInt("track_count", model.track_count);
        bundle.putInt("followers_count", model.followers_count);
        bundle.putInt("followings_count", model.followings_count);
        bundle.putInt("public_likes_count", model.public_likes_count);
        bundle.putInt("private_tracks_count", model.private_tracks_count);
        bundle.putLong("id", model.getId());
        out.writeBundle(bundle);
    }

    public ApiUser toApiMobileUser() {
        final ApiUser apiUser = new ApiUser();
        apiUser.setUrn(getUrn());
        apiUser.setPermalink(getPermalink());
        apiUser.setAvatarUrlTemplate(imageUrlToTemplate(avatar_url).orNull());
        apiUser.setCountry(country);
        apiUser.setCity(city);
        apiUser.setFollowersCount(followers_count);
        apiUser.setUsername(username);
        apiUser.setDescription(description);
        apiUser.setWebsiteUrl(website);
        apiUser.setWebsiteTitle(website_title);
        apiUser.setMyspaceName(myspace_name);
        apiUser.setDiscogsName(discogs_name);
        return apiUser;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setWebsiteTitle(String websiteTitle) {
        this.website_title = websiteTitle;
    }

    public void setDiscogsName(String discogsName) {
        this.discogs_name = discogsName;
    }

    public void setMyspaceName(String myspaceName) {
        this.myspace_name = myspaceName;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrlToTemplate(avatar_url);
    }

    @Override
    public EntityStateChangedEvent toUpdateEvent() {
        return UserItem.from(this).toUpdateEvent();
    }
}
