
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.ScContentProvider;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends BaseObj implements Parcelable {
    private static final String TAG = "Track";

    // API fields

    public long id;
    public String created_at;
    public long user_id;
    public int duration;
    public boolean commentable;
    public String state;
    public String sharing;
    public String tag_list;
    public String permalink;
    public String description;
    public boolean streamable;
    public boolean downloadable;
    public String genre;
    public String release;
    public String purchase_url;
    public String label_id;
    public User label;
    public String label_name;
    public String isrc;
    public String video_url;
    public String track_type;
    public String key_signature;
    public float bpm;

    public int playback_count;
    public int download_count;
    public int comment_count;
    public int favoritings_count;

    public String title;

    public String release_year;
    public String release_month;
    public String release_day;

    public String original_format;
    public String license;

    public String uri;
    public String permalink_url;
    public String artwork_url;
    public String waveform_url;

    public User user;

    public String stream_url; // if track is streamable

    public int user_playback_count; // if user is logged in, 1 or 0
    public boolean user_favorite;   // ditto, and has favorited track

    public CreatedWith created_with;
    public String attachments_uri;


    public String download_url;  // if track downloadable or current user = owner

    // only shown to owner of track
    public int downloads_remaining;
    public String secret_token;
    public String secret_uri;
    public int shared_to_count;

    // Fields used by app

    public boolean user_played;
    public File mCacheFile;
    public Long filelength;


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
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class){
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) != 0);
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

    public String trackInfo() {
        StringBuilder str = new StringBuilder(200);

        if (!TextUtils.isEmpty(description)) {
            str.append(description).append("<br/><br/>");
        }

        for (String t : humanTags()) {
            str.append(t).append("<br/>");
        }

        if (!TextUtils.isEmpty(key_signature)) {
            str.append(key_signature).append("<br/>");
        }
        if (!TextUtils.isEmpty(genre)) {
            str.append(genre).append("<br/>");
        }

        if (bpm != 0) {
            str.append(bpm).append("<br/>");
        }

        str.append("<br/>").append(license()).append("<br/><br/>");

        if (!TextUtils.isEmpty(label_name)) {
            str.append("<b>Released By</b><br/>")
               .append(label_name).append("<br/>");

            if (!TextUtils.isEmpty(release_year)) {
                str.append(release_year).append("<br/>");
            }
            str.append("<br />");
        }
        return str.toString();
    }

    public String license() {
        final String license = TextUtils.isEmpty(this.license) ? "all-rights-reserved" : this.license;
        if (license.startsWith("cc-")) {
            List<String> l = new ArrayList<String>();
            for (String s : license.split("-")) {
                if ("by".equals(s)) l.add("attribution");
                if ("nc".equals(s)) l.add("noncommercial");
                if ("nd".equals(s)) l.add("no derivative work");
                if ("sa".equals(s)) l.add("share alike");
            }
            return TextUtils.join(" ", l);
        } else if ("no-rights-reserved".equals(license)) {
            return "no rights reserved";
        } else {
            return "all rights reserved";
        }
    }
}
