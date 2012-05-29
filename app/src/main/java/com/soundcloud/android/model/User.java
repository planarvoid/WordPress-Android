
package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Users;
import com.soundcloud.android.service.playback.PlaylistManager;
import com.soundcloud.android.utils.ImageUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.EnumSet;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Model
public class User extends ScModel implements  Refreshable, Origin {
    @JsonView(Views.Mini.class) public String username;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public String avatar_url;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Mini.class) public String permalink_url;
    public String full_name;
    public String description;
    public String city;
    public String country;



    public String plan;      // free|lite|solo|pro|pro plus

    public String website;
    public String website_title;
    public String myspace_name;
    public String discogs_name;

    // counts
    public int    track_count            = NOT_SET;
    public int    followers_count        = NOT_SET;
    public int    followings_count       = NOT_SET;
    public int    public_favorites_count = NOT_SET;
    public int    private_tracks_count   = NOT_SET;

    // internal fields
    @JsonIgnore public String _list_avatar_uri;
    @JsonIgnore public boolean user_follower;  // is the user following the logged in user
    @JsonIgnore public boolean user_following; // is the user being followed by the logged in user
    @JsonIgnore public SignupVia via;          // used for tracking

    private Boolean primary_email_confirmed;

    public User() {
    }

    public User(Parcel in) {
        readFromParcel(in);
    }

    public User(Cursor cursor) {
        updateFromCursor(cursor);
    }

    public User updateFromCursor(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(Users._ID));
        permalink = cursor.getString(cursor.getColumnIndex(Users.PERMALINK));
        username = cursor.getString(cursor.getColumnIndex(Users.USERNAME));
        track_count = cursor.getInt(cursor.getColumnIndex(Users.TRACK_COUNT));
        discogs_name = cursor.getString(cursor.getColumnIndex(Users.DISCOGS_NAME));
        city = cursor.getString(cursor.getColumnIndex(Users.CITY));
        avatar_url = cursor.getString(cursor.getColumnIndex(Users.AVATAR_URL));
        full_name = cursor.getString(cursor.getColumnIndex(Users.FULL_NAME));
        followers_count = cursor.getInt(cursor.getColumnIndex(Users.FOLLOWERS_COUNT));
        followings_count = cursor.getInt(cursor.getColumnIndex(Users.FOLLOWINGS_COUNT));
        myspace_name = cursor.getString(cursor.getColumnIndex(Users.MYSPACE_NAME));
        country = cursor.getString(cursor.getColumnIndex(Users.COUNTRY));
        user_follower = cursor.getInt(cursor.getColumnIndex(Users.USER_FOLLOWER)) == 1;
        user_following = cursor.getInt(cursor.getColumnIndex(Users.USER_FOLLOWING)) == 1;
        last_updated = cursor.getLong(cursor.getColumnIndex(Users.LAST_UPDATED));
        description = cursor.getString(cursor.getColumnIndex(Users.DESCRIPTION));
        plan = cursor.getString(cursor.getColumnIndex(Users.PLAN));
        website = cursor.getString(cursor.getColumnIndex(Users.WEBSITE));
        website_title = cursor.getString(cursor.getColumnIndex(Users.WEBSITE_TITLE));
        primary_email_confirmed = cursor.getInt(cursor.getColumnIndex(Users.PRIMARY_EMAIL_CONFIRMED)) == 1;
        public_favorites_count = cursor.getInt(cursor.getColumnIndex(Users.PUBLIC_FAVORITES_COUNT));
        private_tracks_count = cursor.getInt(cursor.getColumnIndex(Users.PRIVATE_TRACKS_COUNT));

        final String tempDesc = cursor.getString(cursor.getColumnIndex(Users.DESCRIPTION));
        if (TextUtils.isEmpty(tempDesc)) description = tempDesc;
        return this;
    }

    public static User fromActivityView(Cursor c) {
        User u = new User();
        u.id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.USER_ID));
        u.username = c.getString(c.getColumnIndex(DBHelper.ActivityView.USER_USERNAME));
        u.permalink = c.getString(c.getColumnIndex(DBHelper.ActivityView.USER_PERMALINK));
        u.avatar_url = c.getString(c.getColumnIndex(DBHelper.ActivityView.USER_AVATAR_URL));
        return u;
    }

    public User(UserlistItem userlistItem) {
        updateFromUserlistItem(userlistItem);
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public ContentValues buildContentValues(){
        return buildContentValues(false);
    }

    public ContentValues buildContentValues(boolean isCurrentUser){
        ContentValues cv = super.buildContentValues();
        // account for partial objects, don't overwrite local full objects
        if (username != null) cv.put(Users.USERNAME, username);
        if (permalink != null) cv.put(Users.PERMALINK, permalink);
        if (avatar_url != null) cv.put(Users.AVATAR_URL, avatar_url);
        if (permalink_url != null) cv.put(Users.PERMALINK_URL, permalink_url);
        if (track_count != NOT_SET) cv.put(Users.TRACK_COUNT, track_count);
        if (public_favorites_count != NOT_SET) cv.put(Users.PUBLIC_FAVORITES_COUNT, public_favorites_count);
        if (city != null) cv.put(Users.CITY, city);
        if (country != null) cv.put(Users.COUNTRY, country);
        if (discogs_name != null) cv.put(Users.DISCOGS_NAME, discogs_name);
        if (full_name != null) cv.put(Users.FULL_NAME, full_name);
        if (myspace_name != null) cv.put(Users.MYSPACE_NAME, myspace_name);
        if (followers_count != NOT_SET) cv.put(Users.FOLLOWERS_COUNT, followers_count);
        if (followings_count != NOT_SET)cv.put(Users.FOLLOWINGS_COUNT, followings_count);
        if (track_count != -1)cv.put(Users.TRACK_COUNT, track_count);
        if (website != null) cv.put(Users.WEBSITE, website);
        if (website_title != null) cv.put(Users.WEBSITE_TITLE, website_title);
        if (plan != null) cv.put(Users.PLAN, plan);
        if (private_tracks_count != NOT_SET) cv.put(Users.PRIVATE_TRACKS_COUNT, private_tracks_count);
        if (primary_email_confirmed != null) cv.put(Users.PRIMARY_EMAIL_CONFIRMED, primary_email_confirmed  ? 1 : 0);

        if (isCurrentUser) {
            if (description != null) cv.put(Users.DESCRIPTION, description);
        }
        cv.put(Users.LAST_UPDATED, System.currentTimeMillis());
        return cv;
    }

    @Override
    public String toString() {
        return "User[" +
                "id=" + id +
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
                ", public_favorites_count='" + public_favorites_count + '\'' +
                ", private_tracks_count='" + private_tracks_count + '\'' +
                ", myspace_name='" + myspace_name + '\'' +
                ", country='" + country + '\'' +
                ", plan='" + plan + '\'' +
                ", primary_email_confirmed=" + primary_email_confirmed +
                ']';
    }

    public Uri toUri() {
        return Content.USERS.buildUpon().appendPath(String.valueOf(id)).build();
    }


    public String getLocation() {
        if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            return city + ", " + country;
        } else if (!TextUtils.isEmpty(city)) {
            return city;
        } else if (!TextUtils.isEmpty(country)) {
            return country;
        } else {
            return "";
        }
    }

    public String getDisplayName() {
        if (!TextUtils.isEmpty(username)){
            return username;
        } else if (!TextUtils.isEmpty(permalink)){
            return permalink;
        } else {
            return "";
        }
    }

    public static User fromTrackView(Cursor cursor) {
        User u = new User();
        u.id = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackView.USER_ID));
        u.username = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.USERNAME));
        u.permalink = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.USER_PERMALINK));
        u.avatar_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.USER_AVATAR_URL));
        return u;
    }

    // setter for deserialization, we want it null if it doesn't exist and to keep it private
    @JsonProperty("primary_email_confirmed")
    public void setPrimaryEmailConfirmed(boolean val){
        primary_email_confirmed = val;
    }

    public boolean isPrimaryEmailConfirmed() {
        return primary_email_confirmed == null || primary_email_confirmed;
    }


    public static interface DataKeys {
        String USERNAME        = "currentUsername";
        String USER_ID         = "currentUserId";
        String USER_PERMALINK  = "currentUserPermalink";
        String DASHBOARD_IDX   = "lastDashboardIndex";
        String PROFILE_IDX     = "lastProfileIndex";
        String SIGNUP          = "signup";

        String LAST_INCOMING_SEEN = "last_incoming_sync_event_timestamp";
        String LAST_OWN_SEEN      = "last_own_sync_event_timestamp";
        String LAST_INCOMING_NOTIFIED_AT = "last_incoming_notified_at_timestamp";

        String LAST_INCOMING_NOTIFIED_ITEM = "last_incoming_notified_timestamp";
        String LAST_OWN_NOTIFIED_ITEM = "last_own_notified_timestamp";

        String FRIEND_FINDER_NO_FRIENDS_SHOWN = "friend_finder_no_friends_shown";
    }

    @Override
    public long getRefreshableId() {
        return id;
    }

    @Override
    public ScModel getRefreshableResource() {
        return this;
    }

    public void refreshListAvatarUri(Context context) {
        final String iconUrl = avatar_url;
        _list_avatar_uri = TextUtils.isEmpty(iconUrl) ? null : Consts.GraphicSize.formatUriForList(context, iconUrl);
    }

    public String getListAvatarUri(Context context){
        if (TextUtils.isEmpty(_list_avatar_uri)) refreshListAvatarUri(context);
        return _list_avatar_uri;
    }

    @Override
    public boolean isStale(){
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.user;
    }

    // TODO, this is kind of dumb.
    public User updateFrom(ScModel updatedItem) {
         if (updatedItem instanceof UserlistItem){
             updateFromUserlistItem((UserlistItem) updatedItem);
         } else if (updatedItem instanceof User){
             updateFromUser((User) updatedItem);
         }
        return this;
    }

    public User updateFromUserlistItem(UserlistItem userlistItem) {
        this.id = userlistItem.id;
        this.username = userlistItem.username;
        this.track_count = userlistItem.track_count;
        this.city = userlistItem.city;
        this.country = userlistItem.country;
        this.avatar_url = userlistItem.avatar_url;
        this.permalink = userlistItem.permalink;
        this.full_name = userlistItem.full_name;
        this.followers_count = userlistItem.followers_count;
        this.followings_count = userlistItem.followings_count;
        this.public_favorites_count = userlistItem.public_favorites_count;
        this.private_tracks_count = userlistItem.private_tracks_count;
        return this;
    }

    public User updateFromUser(User user) {
        this.id = user.id;
        this.username = user.username;
        this.avatar_url = user.avatar_url;
        this.permalink = user.permalink;

        if (user.full_name != null) this.full_name = user.full_name;
        if (user.city != null) this.city = user.city;
        if (user.country != null)this.country = user.country;
        if (user.track_count != -1) this.track_count = user.track_count;
        if (user.followers_count != -1) this.followers_count = user.followers_count;
        if (user.followings_count != -1)this.followings_count = user.followings_count;
        if (user.public_favorites_count != -1) this.public_favorites_count = user.public_favorites_count;
        if (user.private_tracks_count != -1) this.private_tracks_count = user.private_tracks_count;
        if (user.discogs_name != null) this.discogs_name = user.discogs_name;
        if (user.myspace_name != null) this.myspace_name = user.myspace_name;
        if (user.description != null) this.description = user.description;
        if (user.primary_email_confirmed != null) this.primary_email_confirmed = user.primary_email_confirmed;
        return this;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return this;
    }

    public void resolve(SoundCloudApplication application) {
        refreshListAvatarUri(application);
    }

    public void setAppFields(User u) {
        user_follower = u.user_follower;
        user_following = u.user_following;
    }

    public boolean shouldLoadIcon() {
        return ImageUtils.checkIconShouldLoad(avatar_url);
    }

    public Plan getPlan() {
        return Plan.fromApi(plan);
    }

    public static void clearLoggedInUserFromStorage(SoundCloudApplication app) {
        final ContentResolver resolver = app.getContentResolver();
        // TODO move to model
        for (Content c : EnumSet.of(
                Content.ME_TRACKS,
                Content.ME_FAVORITES,
                Content.ME_FOLLOWINGS,
                Content.ME_FOLLOWERS)) {
            resolver.delete(Content.COLLECTIONS.uri,
                DBHelper.Collections.URI + " = ?", new String[]{ c.uri.toString() });
        }
        Activities.clear(null, resolver);
        PlaylistManager.clearState(app);
        Search.clearState(resolver, SoundCloudApplication.getUserId());
    }

    @Override
    protected void readFromParcel(Parcel in) {
        // TODO replace with generated file
        User model = this;
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
        model.public_favorites_count = bundle.getInt("public_favorites_count");
        model.private_tracks_count = bundle.getInt("private_tracks_count");
        model.id = bundle.getLong("id");
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        // TODO replace with generated file
        Bundle bundle = new Bundle();
        User model = this;
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
        bundle.putInt("public_favorites_count", model.public_favorites_count);
        bundle.putInt("private_tracks_count", model.private_tracks_count);
        bundle.putLong("id", model.id);

        out.writeBundle(bundle);
    }
}
