package com.soundcloud.android.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.provider.DatabaseHelper.Users;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserlistItem {
    public long id;
    public String username;
    public int track_count;
    public String city;
    public String avatar_url;
    public String permalink;
    public String full_name;
    public int followers_count;
    public int followings_count;
    public int public_favorites_count;
    public int private_tracks_count;
    public String country;
}