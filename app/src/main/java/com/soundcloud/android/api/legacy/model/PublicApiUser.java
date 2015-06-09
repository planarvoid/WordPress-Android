package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Optional;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Model;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.profile.LegacyProfileActivity;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

@Deprecated
@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Model
public class PublicApiUser extends PublicApiResource implements UserHolder, PropertySetSource, UserRecord {
    public static final int CRAWLER_USER_ID = -2;
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

    public static final PublicApiUser CRAWLER_USER = new PublicApiUser(CRAWLER_USER_ID, "SoundCloud");

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
    @Nullable @JsonIgnore public String _list_avatar_uri;
    @JsonIgnore public boolean user_follower;  // is the user following the logged in user
    @JsonIgnore public boolean user_following; // is the user being followed by the logged in user
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

    public PublicApiUser(SuggestedUser suggestedUser) {
        setUrn(suggestedUser.getUrn().toString());
        username = suggestedUser.getUsername();
        city = suggestedUser.getCity();
        country = suggestedUser.getCountry();
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

    public PublicApiUser(Cursor cursor) {
        updateFromCursor(cursor);
    }

    public PublicApiUser(UserRecord user) {
        setUrn(user.getUrn().toString());
        setUsername(user.getUsername());
        avatar_url = user.getAvatarUrl();
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        urn = Urn.forUser(id);
    }

    public PublicApiUser updateFromCursor(Cursor cursor) {
        setId(cursor.getLong(cursor.getColumnIndex(TableColumns.Users._ID)));
        permalink = cursor.getString(cursor.getColumnIndex(TableColumns.Users.PERMALINK));
        permalink_url = cursor.getString(cursor.getColumnIndex(TableColumns.Users.PERMALINK_URL));
        username = cursor.getString(cursor.getColumnIndex(TableColumns.Users.USERNAME));
        track_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.TRACK_COUNT));
        discogs_name = cursor.getString(cursor.getColumnIndex(TableColumns.Users.DISCOGS_NAME));
        city = cursor.getString(cursor.getColumnIndex(TableColumns.Users.CITY));
        avatar_url = cursor.getString(cursor.getColumnIndex(TableColumns.Users.AVATAR_URL));
        full_name = cursor.getString(cursor.getColumnIndex(TableColumns.Users.FULL_NAME));
        followers_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.FOLLOWERS_COUNT));
        followings_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.FOLLOWINGS_COUNT));
        myspace_name = cursor.getString(cursor.getColumnIndex(TableColumns.Users.MYSPACE_NAME));
        country = cursor.getString(cursor.getColumnIndex(TableColumns.Users.COUNTRY));
        user_follower = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.USER_FOLLOWER)) == 1;
        user_following = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.USER_FOLLOWING)) == 1;
        last_updated = cursor.getLong(cursor.getColumnIndex(TableColumns.Users.LAST_UPDATED));
        description = cursor.getString(cursor.getColumnIndex(TableColumns.Users.DESCRIPTION));
        plan = cursor.getString(cursor.getColumnIndex(TableColumns.Users.PLAN));
        website = cursor.getString(cursor.getColumnIndex(TableColumns.Users.WEBSITE_URL));
        website_title = cursor.getString(cursor.getColumnIndex(TableColumns.Users.WEBSITE_NAME));
        primary_email_confirmed = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.PRIMARY_EMAIL_CONFIRMED)) == 1;
        public_likes_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.PUBLIC_LIKES_COUNT));
        private_tracks_count = cursor.getInt(cursor.getColumnIndex(TableColumns.Users.PRIVATE_TRACKS_COUNT));

        final String tempDesc = cursor.getString(cursor.getColumnIndex(TableColumns.Users.DESCRIPTION));
        if (TextUtils.isEmpty(tempDesc)) {
            description = tempDesc;
        }
        return this;
    }

    public static PublicApiUser fromActivityView(Cursor c) {
        PublicApiUser u = new PublicApiUser();
        u.setId(c.getLong(c.getColumnIndex(TableColumns.ActivityView.USER_ID)));
        u.username = c.getString(c.getColumnIndex(TableColumns.ActivityView.USER_USERNAME));
        u.permalink = c.getString(c.getColumnIndex(TableColumns.ActivityView.USER_PERMALINK));
        u.avatar_url = c.getString(c.getColumnIndex(TableColumns.ActivityView.USER_AVATAR_URL));
        return u;
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        // account for partial objects, don't overwrite local full objects
        if (username != null) {
            cv.put(TableColumns.Users.USERNAME, username);
        }
        if (permalink != null) {
            cv.put(TableColumns.Users.PERMALINK, permalink);
        }
        if (avatar_url != null) {
            cv.put(TableColumns.Users.AVATAR_URL, avatar_url);
        }
        if (permalink_url != null) {
            cv.put(TableColumns.Users.PERMALINK_URL, permalink_url);
        }
        if (track_count != NOT_SET) {
            cv.put(TableColumns.Users.TRACK_COUNT, track_count);
        }
        if (public_likes_count != NOT_SET) {
            cv.put(TableColumns.Users.PUBLIC_LIKES_COUNT, public_likes_count);
        }
        if (city != null) {
            cv.put(TableColumns.Users.CITY, city);
        }
        if (country != null) {
            cv.put(TableColumns.Users.COUNTRY, country);
        }
        if (discogs_name != null) {
            cv.put(TableColumns.Users.DISCOGS_NAME, discogs_name);
        }
        if (full_name != null) {
            cv.put(TableColumns.Users.FULL_NAME, full_name);
        }
        if (myspace_name != null) {
            cv.put(TableColumns.Users.MYSPACE_NAME, myspace_name);
        }
        if (followers_count != NOT_SET) {
            cv.put(TableColumns.Users.FOLLOWERS_COUNT, followers_count);
        }
        if (followings_count != NOT_SET) {
            cv.put(TableColumns.Users.FOLLOWINGS_COUNT, followings_count);
        }
        if (track_count != -1) {
            cv.put(TableColumns.Users.TRACK_COUNT, track_count);
        }
        if (website != null) {
            cv.put(TableColumns.Users.WEBSITE_URL, website);
        }
        if (website_title != null) {
            cv.put(TableColumns.Users.WEBSITE_NAME, website_title);
        }
        if (plan != null) {
            cv.put(TableColumns.Users.PLAN, plan);
        }
        if (private_tracks_count != NOT_SET) {
            cv.put(TableColumns.Users.PRIVATE_TRACKS_COUNT, private_tracks_count);
        }
        if (primary_email_confirmed != null) {
            cv.put(TableColumns.Users.PRIMARY_EMAIL_CONFIRMED, primary_email_confirmed ? 1 : 0);
        }

        if (getId() != -1 && getId() == SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId()) {
            if (description != null) {
                cv.put(TableColumns.Users.DESCRIPTION, description);
            }
        }
        cv.put(TableColumns.Users.LAST_UPDATED, System.currentTimeMillis());
        return cv;
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

    @Override
    public Uri toUri() {
        return Content.USERS.forId(getId());
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

    public String getDisplayName() {
        if (!TextUtils.isEmpty(username)) {
            return username;
        } else if (!TextUtils.isEmpty(permalink)) {
            return permalink;
        } else {
            return ScTextUtils.EMPTY_STRING;
        }
    }

    public static PublicApiUser fromSoundView(Cursor cursor) {
        PublicApiUser u = new PublicApiUser();
        u.setId(cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.USER_ID)));
        u.username = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.USERNAME));
        u.permalink = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.USER_PERMALINK));
        u.avatar_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.USER_AVATAR_URL));
        return u;
    }

    public boolean isPrimaryEmailConfirmed() {
        return primary_email_confirmed == null || primary_email_confirmed;
    }

    // setter for deserialization, we want it null if it doesn't exist and to keep it private
    @JsonProperty("primary_email_confirmed")
    public void setPrimaryEmailConfirmed(boolean val) {
        primary_email_confirmed = val;
    }

    public Intent getViewIntent() {
        return new Intent(Actions.USER_BROWSER).putExtra(LegacyProfileActivity.EXTRA_USER, this);
    }

    public boolean addAFollower() {
        if (isFollowersCountSet()) {
            followers_count++;
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAFollower() {
        if (isFollowersCountSet()) {
            followers_count--;
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    @Override
    public PublicApiUser getUser() {
        return this;
    }

    @Nullable
    public String getCity() {
        return city;
    }

    public final void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public String getCountry() {
        return country;
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

    public final void setCountry(@Nullable String country) {
        this.country = country;
    }

    public String getLocation() {
        return ScTextUtils.getLocation(city, country);
    }

    @Override
    public PropertySet toPropertySet() {
        final PropertySet propertySet = PropertySet.from(
                UserProperty.URN.bind(getUrn()),
                UserProperty.USERNAME.bind(username),
                UserProperty.FOLLOWERS_COUNT.bind(followers_count)
        );
        if (country != null) {
            propertySet.put(UserProperty.COUNTRY, country);
        }
        return propertySet;
    }

    @Override
    public Refreshable getRefreshableResource() {
        return this;
    }

    public boolean hasAvatarUrl() {
        return !TextUtils.isEmpty(avatar_url);
    }

    public String getNonDefaultAvatarUrl() {
        return shouldLoadIcon() ? avatar_url : null;
    }

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.USER;
    }

    @Override
    public boolean isIncomplete() {
        return getId() <= 0;
    }

    public boolean isCrawler() {
        return getId() == CRAWLER_USER_ID;
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public PublicApiUser updateFrom(PublicApiUser user, CacheUpdateMode cacheUpdateMode) {
        this.setId(user.getId());
        this.username = user.username;

        if (user.avatar_url != null) {
            this.avatar_url = user.avatar_url;
        }
        if (user.permalink != null) {
            this.permalink = user.permalink;
        }
        if (user.full_name != null) {
            this.full_name = user.full_name;
        }
        if (user.city != null) {
            this.city = user.city;
        }
        if (user.country != null) {
            this.country = user.country;
        }
        if (user.track_count != -1) {
            this.track_count = user.track_count;
        }
        if (user.followers_count != -1) {
            this.followers_count = user.followers_count;
        }
        if (user.followings_count != -1) {
            this.followings_count = user.followings_count;
        }
        if (user.public_likes_count != -1) {
            this.public_likes_count = user.public_likes_count;
        }
        if (user.private_tracks_count != -1) {
            this.private_tracks_count = user.private_tracks_count;
        }
        if (user.discogs_name != null) {
            this.discogs_name = user.discogs_name;
        }
        if (user.myspace_name != null) {
            this.myspace_name = user.myspace_name;
        }
        if (user.description != null) {
            this.description = user.description;
        }
        if (user.primary_email_confirmed != null) {
            this.primary_email_confirmed = user.primary_email_confirmed;
        }

        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            last_updated = user.last_updated;
        }
        return this;
    }

    public void setAppFields(PublicApiUser u) {
        user_follower = u.user_follower;
        user_following = u.user_following;
    }

    public boolean shouldLoadIcon() {
        return ImageUtils.checkIconShouldLoad(avatar_url);
    }

    public boolean isFollowersCountSet() {
        return followers_count > NOT_SET;
    }

    public Plan getPlan() {
        return Plan.fromApi(plan);
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.USERS.uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public
    @Nullable
    String getWebSiteTitle() {
        if (!TextUtils.isEmpty(website_title)) {
            return website_title;
        } else if (!TextUtils.isEmpty(website)) {
            return website.replace("http://www.", "")
                    .replace("http://", "");
        } else {
            return null;
        }
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
        final ApiUser apiUser = new ApiUser(urn);
        apiUser.setAvatarUrl(avatar_url);
        apiUser.setCountry(country);
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


    public interface DataKeys {
        String FRIEND_FINDER_NO_FRIENDS_SHOWN = "friend_finder_no_friends_shown";
        String SEEN_CREATE_AUTOSAVE = "seenCreateAutoSave";

    }
}
