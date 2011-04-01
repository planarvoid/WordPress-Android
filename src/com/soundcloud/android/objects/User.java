
package com.soundcloud.android.objects;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB.Users;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Field;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends BaseObj implements Parcelable {
    public long id;
    public String username;
    public String track_count;
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
    public String followers_count;    // XXX int
    public String followings_count;   // XXX int
    public String public_favorites_count; // XXX int
    public String private_tracks_count; // XXX int
    public String myspace_name;
    public String country;
    public String location;
    public String plan;

    public boolean primary_email_confirmed;
    public boolean current_user_following;

    public void resolveLocation() {
        this.location = CloudUtils.getLocationString(city == null ? "" : city,
        country == null ? "" : country);
    }

    public User() {
    }

    public User(Parcel in) {
        readFromParcel(in);
    }


    public User(Cursor cursor) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (key.contentEquals("_id")) id = cursor.getLong(cursor.getColumnIndex(key));
            else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == boolean.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
    }

    public User (SharedPreferences preferences){
        id = preferences.getLong(SoundCloudApplication.Prefs.USER_ID, -1);
        username = preferences.getString(SoundCloudApplication.Prefs.USERNAME, "");
        primary_email_confirmed = preferences.getBoolean(SoundCloudApplication.Prefs.EMAIL_CONFIRMED, false);
    }

    public void update(Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                if (key.contentEquals("_id")) id = cursor.getLong(cursor.getColumnIndex(key));
                else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            if (f.get(this) == null)
                                f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            if (f.get(this) == null)
                                f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == boolean.class) {
                            if (f.get(this) == null)
                                f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
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
                ", location='" + location + '\'' +
                ", plan='" + plan + '\'' +
                ", primary_email_confirmed=" + primary_email_confirmed +
                ", current_user_following=" + current_user_following +
                ']';
    }
}
