
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.ScContentProvider;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends BaseObj implements Parcelable {
    private static final String TAG = "Track";

    public long id;
    public String artwork_url;
    public String attachments_uri;
    public String avatar_url;
    public Float bpm;
    public boolean commentable;
    public int comment_count;
    public String created_at;
    public CreatedWith created_with;
    public String description;
    public boolean downloadable;
    public int download_count;
    public String download_url;
    public int downloads_remaining;
    public int duration;
    public String duration_formatted;
    public int favoritings_count;
    public String genre;
    public String isrc;
    public String key_signature;
    public User label;
    public String label_id;
    public String label_name;
    public String license;
    public String original_format;
    public String permalink;
    public String permalink_url;
    public String playback_count;
    public String purchase_url;
    public String release;
    public String release_day;
    public String release_month;
    public String release_year;
    public String secret_token;
    public String secret_uri;
    public int shared_to_count;
    public String sharing;
    public String state;
    public boolean streamable;
    public String stream_url;
    public String tag_list;
    public String track_type;
    public String title;
    public String uri;
    public boolean user_played;
    public String user_playback_count;
    public boolean user_favorite;
    public long user_favorite_id;
    public User user;
    public long user_id;
    public String video_url;
    public String waveform_url;

    public List<String> humanTags() {
        List<String> tags = new ArrayList<String>();
        if (tag_list == null) return tags;
        for (String t : tag_list.split("\\s")) {
            if (t.indexOf(':') == -1 && t.indexOf('=') == -1) {
                tags.add(t);
            }
        }
        return tags;
    }

    public static class CreatedWith {
        public long id;
        public String name;
        public String uri;
        public String permalink_url;
    }

    public boolean mIsPlaylist = false;
    public File mCacheFile;
    public Long filelength;
    public String mSignedUri;

    public static final class Tracks implements BaseColumns {
        private Tracks() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Tracks");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.tracks";
        public static final String ID = "_id";
        public static final String PERMALINK = "permalink";
        public static final String DURATION = "duration";
        public static final String TAG_LIST = "tag_list";
        public static final String TRACK_TYPE = "track_type";
        public static final String TITLE = "title";
        public static final String PERMALINK_URL = "permalink_url";
        public static final String ARTWORK_URL = "artwork_url";
        public static final String WAVEFORM_URL = "waveform_url";
        public static final String DOWNLOADABLE = "downloadable";
        public static final String DOWNLOAD_URL = "download_url";
        public static final String STREAM_URL = "stream_url";
        public static final String STREAMABLE = "streamable";
        public static final String USER_ID = "user_id";
        public static final String USER_FAVORITE = "user_favorite";
        public static final String USER_PLAYED = "user_played";
        public static final String FILELENGTH = "filelength";
    }

    public Track() {
    }

    public Track(Parcel in) {
        readFromParcel(in);
    }

    public Track(Cursor cursor) {
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
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) == 0 ? false : true);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "error", e);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "error", e);
                } catch (SecurityException e) {
                    Log.e(TAG, "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
    }

    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
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

}
