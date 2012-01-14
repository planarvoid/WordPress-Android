
package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Users;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonView;

import static com.soundcloud.android.SoundCloudApplication.TAG;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends ScModel implements PageTrackable, Resource {
    @JsonView(Views.Mini.class) public String username;
    public int track_count;
    public String discogs_name;
    public String city;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public String avatar_url;
    public String local_avatar_url;
    public String website_title;
    public String website;
    public String description;
    public String online;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Mini.class) public String permalink_url;
    public String full_name;
    public int followers_count = -1;
    public int followings_count = -1;
    public int public_favorites_count = -1;
    public int private_tracks_count = -1;
    public String myspace_name;
    public String country;
    public String plan;

    public boolean user_follower; // is the user following the logged in user
    public boolean user_following; // is the user being followed by the logged in user

    public boolean primary_email_confirmed;
    @JsonIgnore public long last_updated;

    public User() {
    }

    public User(Parcel in) {
        readFromParcel(in);
    }

    public User(Cursor cursor) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (key.contentEquals(Users.ID)) {
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                try {
                    setFieldFromCursor(this,
                            User.class.getDeclaredField(key),
                            cursor, key);
                } catch (SecurityException e) {
                    Log.e(TAG, "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
    }

    public User(UserlistItem userlistItem) {
        updateFromUserlistItem(userlistItem);
    }

    public void assertInDb(SoundCloudApplication app) {
        SoundCloudDB.writeUser(app.getContentResolver(), this, SoundCloudDB.WriteState.insert_only, app.getCurrentUserId());
    }

    public void updateFromDb(ContentResolver contentResolver, Long currentUserId) {
        Cursor cursor = contentResolver.query(appendIdToUri(Content.USERS.uri), null, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String[] keys = cursor.getColumnNames();
                for (String key : keys) {
                    if (key.contentEquals("_id")) {
                        id = cursor.getLong(cursor.getColumnIndex(key));
                    } else {
                        try {
                            setFieldFromCursor(this, User.class.getDeclaredField(key), cursor,
                                    key);
                        } catch (SecurityException e) {
                            Log.e(TAG, "error", e);
                        } catch (NoSuchFieldException e) {
                            Log.e(TAG, "error", e);
                        }
                    }
                }
            }
            cursor.close();
        }
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
        ContentValues cv = new ContentValues();
        cv.put(Users.ID, id);
        cv.put(Users.USERNAME, username);
        cv.put(Users.PERMALINK, permalink);
        cv.put(Users.AVATAR_URL, avatar_url);
        // account for partial objects, don't overwrite local full objects
        if (track_count != -1) cv.put(Users.LAST_UPDATED, System.currentTimeMillis());
        if (city != null) cv.put(Users.CITY, city);
        if (country != null) cv.put(Users.COUNTRY, country);
        if (discogs_name != null) cv.put(Users.DISCOGS_NAME, discogs_name);
        if (full_name != null) cv.put(Users.FULL_NAME, full_name);
        if (myspace_name != null) cv.put(Users.MYSPACE_NAME, myspace_name);
        if (followers_count != -1) cv.put(Users.FOLLOWERS_COUNT, followers_count);
        if (followings_count != -1)cv.put(Users.FOLLOWINGS_COUNT, followings_count);
        if (track_count != -1)cv.put(Users.TRACK_COUNT, track_count);
        if (website != null) cv.put(Users.WEBSITE, website);
        if (website_title != null) cv.put(Users.WEBSITE_TITLE, website_title);
        if (isCurrentUser && description != null) cv.put(Users.DESCRIPTION, description);
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
                ", local_avatar_url='" + local_avatar_url + '\'' +
                ", website_title='" + website_title + '\'' +
                ", website='" + website + '\'' +
                ", description='" + description + '\'' +
                ", online='" + online + '\'' +
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

    @Override
    public String pageTrack(String... paths) {
        return pageTrack(false, paths);
    }

    public String pageTrack(boolean currentUser, String... paths) {
        StringBuilder sb = new StringBuilder();
        sb.append("/").append(currentUser ? "you" : permalink);
        for (String p : paths) {
            sb.append("/").append(p);
        }
        return sb.toString();
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

    public static interface DataKeys {
        String USERNAME = "currentUsername";
        String USER_ID = "currentUserId";
        String EMAIL_CONFIRMED = "email_confirmed";
        String DASHBOARD_IDX = "lastDashboardIndex";
        String PROFILE_IDX = "lastProfileIndex";

        // legacy
        String OAUTH1_ACCESS_TOKEN = "oauth_access_token";
        String OAUTH1_ACCESS_TOKEN_SECRET = "oauth_access_token_secret";

        String LAST_INCOMING_SEEN = "last_incoming_sync_event_timestamp";
        String LAST_OWN_SEEN      = "last_own_sync_event_timestamp";

        String LAST_INCOMING_NOTIFIED_AT = "last_incoming_notified_at_timestamp";
        String LAST_OWN_NOTIFIED_AT = "last_own_notified_at_timestamp";

        String LAST_INCOMING_NOTIFIED_ITEM = "last_incoming_notified_timestamp";
        String LAST_OWN_NOTIFIED_ITEM = "last_own_notified_timestamp";

        String FRIEND_FINDER_NO_FRIENDS_SHOWN = "friend_finder_no_friends_shown";
    }

    @Override
    public long getResourceId() {
        return id;
    }

    @Override
    public long getLastUpdated(){
        return last_updated;
    }

    @Override
    public long getStaleTime() {
        return 86400000;//24*60*60*1000 = 24hr
    }


    public User updateFrom(ScModel updatedItem) {
         if (updatedItem instanceof UserlistItem){
             updateFromUserlistItem((UserlistItem) updatedItem);
         }
        return this;
    }

    private void updateFromUserlistItem(UserlistItem userlistItem) {
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
    }
}
