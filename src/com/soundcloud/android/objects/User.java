
package com.soundcloud.android.objects;

import java.lang.reflect.Field;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends BaseObj implements Parcelable {

    public Long id;

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

    public String followers_count;

    public String followings_count;

    public String public_favorites_count;

    public String private_tracks_count;
    
    public String myspace_name;

    public String country;

    public String location;

    public String plan;

    public String user_following;

    public String user_following_id;

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
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();

            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.class) {
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

    public void update(Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            if (f.get(this) == null)
                                f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.class) {
                            if (f.get(this) == null)
                                f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.class) {
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
    public int describeContents() {
        return 0;
    }

}
