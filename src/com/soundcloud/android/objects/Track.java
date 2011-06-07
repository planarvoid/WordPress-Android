
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends BaseObj implements Parcelable {
    private static final String TAG = "Track";

    // API fields

    public long id;
    public Date created_at;
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

    public String stream_url;

    public int user_playback_count;
    public boolean user_favorite;

    public CreatedWith created_with;
    public String attachments_uri;

    public String download_url;  // if track downloadable or current user = owner

    // only shown to owner of track
    public int downloads_remaining;
    public String secret_token;
    public String secret_uri;
    public int shared_to_count;

    // Fields used by app
    @JsonIgnore
    public List<Comment> comments;
    @JsonIgnore
    public File mCacheFile;
    @JsonIgnore
    public long filelength;
    @JsonIgnore
    public boolean user_played;
    @JsonIgnore
    public LoadTrackInfoTask load_info_task;
    @JsonIgnore
    public LoadCommentsTask load_comments_task;
    @JsonIgnore
    public boolean info_loaded;
    @JsonIgnore
    public boolean comments_loaded;


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

    public Uri toUri() {
        return Content.TRACKS.buildUpon().appendPath(String.valueOf(id)).build();
    }

    public static class CreatedWith {
        public long id;
        public String name;
        public String uri;
        public String permalink_url;
    }

    public Track() {
    }

    public Track(Parcel in) {
        readFromParcel(in);
    }

    public Track(Cursor cursor, boolean aliasesOnly ) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (aliasesOnly && !key.contains(Tables.TRACKS+"_")) continue;
            if (key.contentEquals(aliasesOnly ? Tracks.ALIAS_ID : Tracks.ID)){
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                    try {
                        setFieldFromCursor(this,this.getClass().getDeclaredField(aliasesOnly ? key.substring(7) : key),cursor,key);
                    } catch (SecurityException e) {
                        Log.e(TAG, "error", e);
                    } catch (NoSuchFieldException e) {
                        Log.e(TAG, "error", e);
                    }
            }
        }
    }

    public void updateFromDb(ContentResolver contentResolver, Long currentUserId) {
        Cursor cursor = contentResolver.query(Content.TRACKS, null, Tracks.ID + " = ?", new String[] { Long.toString(id) },
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
                            setFieldFromCursor(this, this.getClass().getDeclaredField(key), cursor,
                                    key);
                        } catch (SecurityException e) {
                            Log.e(TAG, "error", e);
                        } catch (NoSuchFieldException e) {
                            Log.e(TAG, "error", e);
                        }
                    }
                }
                if (!user_played) {
                    this.updateUserPlayedFromDb(contentResolver, currentUserId);
                }
            }
            cursor.close();
        }
        if (user != null) {
            user.updateFromDb(contentResolver, currentUserId);
        }
    }

    public void updateUserPlayedFromDb(ContentResolver contentResolver, Long currentUserId) {
        Cursor cursor = contentResolver.query(Content.TRACK_PLAYS, null, TrackPlays.TRACK_ID
                + "= ? AND " + TrackPlays.USER_ID + " = ?", new String[] {
                Long.toString(id), Long.toString(currentUserId) }, null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                user_played = true;
            }
            cursor.close();
        }
    }

    public void setAppFields(Track t) {
        comments = t.comments;
        mCacheFile = t.mCacheFile;
        filelength = t.filelength;
        user_played = t.user_played;
        info_loaded = t.info_loaded;
        comments_loaded = t.comments_loaded;
    }

    public boolean isStreamable() {
        return streamable && stream_url != null;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ContentValues buildContentValues(){
        ContentValues cv = new ContentValues();
        cv.put(Tracks.ID, id);
        cv.put(Tracks.PERMALINK, permalink);
        cv.put(Tracks.DURATION, duration);
        cv.put(Tracks.CREATED_AT, created_at.getTime());
        cv.put(Tracks.TAG_LIST, tag_list);
        cv.put(Tracks.TRACK_TYPE, track_type);
        cv.put(Tracks.TITLE, title);
        cv.put(Tracks.PERMALINK_URL, permalink_url);
        cv.put(Tracks.ARTWORK_URL, artwork_url);
        cv.put(Tracks.WAVEFORM_URL, waveform_url);
        cv.put(Tracks.DOWNLOADABLE, downloadable);
        cv.put(Tracks.DOWNLOAD_URL, download_url);
        cv.put(Tracks.STREAM_URL, stream_url);
        cv.put(Tracks.STREAMABLE, streamable);
        cv.put(Tracks.SHARING, sharing);
        cv.put(Tracks.USER_ID, user_id);
        // app level, only add these 2 if they have been set, otherwise they
        // might overwrite valid db values
        if (user_favorite) cv.put(Tracks.USER_FAVORITE, user_favorite);
        if (filelength > 0) cv.put(Tracks.FILELENGTH, filelength);
        return cv;
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

        str.append("<br/>").append(formattedLicense()).append("<br/><br/>");

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

    public String formattedLicense() {
        final StringBuilder sb = new StringBuilder(200);
        final String license = TextUtils.isEmpty(this.license) ? "all-rights-reserved" : this.license;
        if (license.startsWith("cc-")) {
            String cc = license.substring(3, license.length());

            sb.append("Licensed under a Creative Commons License ");
            sb.append('(').append("<a href='").append(getCCLink(cc)).append("'>")
               .append(cc.toUpperCase())
               .append("</a>")
               .append(')');
        } else if ("no-rights-reserved".equals(license)) {
            sb.append("No Rights Reserved");
        }
        return sb.toString();
    }

    private String getCCLink(String license) {
        return "http://creativecommons.org/licenses/"+license+"/3.0";
    }


    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
}
