
package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.provider.DatabaseHelper.Users;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends BaseObj implements Parcelable {
    public long id;
    public String username;
    public int track_count;
    public String discogs_name;
    public String city;
    public String uri;
    public String avatar_url;
    public String local_avatar_url;
    public String website_title;
    public String website;
    public String description;
    public String online;
    public String permalink;
    public String permalink_url;
    public String full_name;
    public int followers_count;
    public int followings_count;
    public int public_favorites_count;
    public int private_tracks_count;
    public String myspace_name;
    public String country;
    public String plan;

    public boolean primary_email_confirmed;

    public User() {
    }

    public User(Parcel in) {
        readFromParcel(in);
    }

    public User(SoundCloudApplication scApp){
        id = scApp.getAccountDataLong(DataKeys.USER_ID);
        username = scApp.getAccountData(DataKeys.USERNAME);
        primary_email_confirmed = scApp.getAccountDataBoolean(DataKeys.EMAIL_CONFIRMED);
    }

    public User(Cursor cursor, boolean aliasesOnly) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (aliasesOnly && !key.contains(Tables.USERS + "_")) continue;
            if (key.contentEquals(aliasesOnly ? Users.ALIAS_ID : Users.ID)) {
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                try {
                    setFieldFromCursor(this,
                            User.class.getDeclaredField(aliasesOnly ? key.substring(6) : key),
                            cursor, key);
                } catch (SecurityException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                    e.printStackTrace();
                }
            }
        }
    }

    public User(UserlistItem userlistItem) {
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

    public void updateFromDb(ContentResolver contentResolver, Long currentUserId) {
        Cursor cursor = contentResolver.query(Content.USERS, null, Users.ID + "= ?",new String[]{Long.toString(id)},
                null);
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
                            Log.e(getClass().getSimpleName(), "error", e);
                            e.printStackTrace();
                        } catch (NoSuchFieldException e) {
                            Log.e(getClass().getSimpleName(), "error", e);
                            e.printStackTrace();
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

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ContentValues buildContentValues(boolean isCurrentUser){
        ContentValues cv = new ContentValues();
        cv.put(Users.ID, id);
        cv.put(Users.USERNAME, username);
        cv.put(Users.PERMALINK, permalink);
        cv.put(Users.AVATAR_URL, avatar_url);
        cv.put(Users.CITY, city);
        cv.put(Users.COUNTRY, country);
        cv.put(Users.DISCOGS_NAME, discogs_name);
        cv.put(Users.FOLLOWERS_COUNT, followers_count);
        cv.put(Users.FOLLOWINGS_COUNT, followings_count);
        cv.put(Users.FULL_NAME, full_name);
        cv.put(Users.MYSPACE_NAME, myspace_name);
        cv.put(Users.TRACK_COUNT, track_count);
        cv.put(Users.WEBSITE, website);
        cv.put(Users.WEBSITE_TITLE, website_title);
        if (isCurrentUser) cv.put(Users.DESCRIPTION, description);
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

    public static interface DataKeys {
        String USERNAME = "currentUsername";
        String USER_ID = "currentUserId";
        String EMAIL_CONFIRMED = "email_confirmed";
        String DASHBOARD_IDX = "lastDashboardIndex";
        String PROFILE_IDX = "lastProfileIndex";
        // legacy
        String OAUTH1_ACCESS_TOKEN = "oauth_access_token";
        String OAUTH1_ACCESS_TOKEN_SECRET = "oauth_access_token_secret";

        String LAST_FOLLOWINGS_SYNC = "last_followings_sync";

        String LAST_INCOMING_SYNC = "last_incoming_sync";
        String LAST_EXCLUSIVE_SYNC = "last_exclusive_sync";

        String CURRENT_INCOMING_UNSEEN = "current_incoming_unseen";
        String CURRENT_EXCLUSIVE_UNSEEN = "current_exclusive_unseen";
        String FRIEND_FINDER_NO_FRIENDS_SHOWN = "friend_finder_no_friends_shown";
    }
}
