
package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.TracksByTag;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Tables;
import com.soundcloud.android.provider.DatabaseHelper.TrackPlays;
import com.soundcloud.android.provider.DatabaseHelper.Tracks;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.FlowLayout;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends ModelBase implements PageTrackable, Origin {
    private static final String TAG = "Track";

    // API fields
    public Date created_at;
    @JsonView(Views.Mini.class) public long user_id;
    public int duration;
    public boolean commentable;
    public String state;
    public String sharing;
    public String tag_list;
    @JsonView(Views.Mini.class) public String permalink;
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

    @JsonView(Views.Mini.class) public String title;

    public String release_year;
    public String release_month;
    public String release_day;

    public String original_format;
    public String license;

    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public String user_uri;
    @JsonView(Views.Mini.class) public String permalink_url;
    public String artwork_url;
    public String waveform_url;

    public User user;

    @JsonView(Views.Mini.class) public String stream_url;

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
    protected File mCacheFile;
    private CharSequence mElapsedTime;

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

    @Override @JsonIgnore
    public Track getTrack() {
        return this;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
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

    public Track(TracklistItem tracklistItem) {
        id = tracklistItem.id;
        title = tracklistItem.title;
        created_at = tracklistItem.created_at;
        user_id = tracklistItem.user_id;
        duration = tracklistItem.duration;
        commentable = tracklistItem.commentable;
        sharing = tracklistItem.sharing;
        permalink = tracklistItem.permalink;
        streamable = tracklistItem.streamable;
        artwork_url = tracklistItem.artwork_url;
        waveform_url = tracklistItem.waveform_url;
        user = tracklistItem.user;
        stream_url = tracklistItem.stream_url;
        playback_count = tracklistItem.playback_count;
        comment_count = tracklistItem.comment_count;
        favoritings_count = tracklistItem.favoritings_count;
        user_favorite = tracklistItem.user_favorite;
    }

    public Track(Cursor cursor, boolean aliasesOnly ) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (aliasesOnly && !key.contains(Tables.TRACKS+"_")) continue;
            if (key.contentEquals(aliasesOnly ? Tracks.ALIAS_ID : Tracks.ID)){
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                    try {
                        setFieldFromCursor(this,Track.class.getDeclaredField(aliasesOnly ? key.substring(7) : key),cursor,key);
                    } catch (SecurityException e) {
                        Log.e(TAG, "error", e);
                    } catch (NoSuchFieldException e) {
                        Log.e(TAG, "error", e);
                    }
            }
        }
    }

    public void updateFromDb(ContentResolver resolver, User user) {
        updateFromDb(resolver, user.id);
    }

    public void updateFromDb(ContentResolver resolver, long currentUserId) {
        Cursor cursor = resolver.query(Content.TRACKS, null, Tracks.ID + " = ?",
                new String[] { Long.toString(id) }, null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String[] keys = cursor.getColumnNames();
                for (String key : keys) {
                    if (key.contentEquals("_id")) {
                        id = cursor.getLong(cursor.getColumnIndex(key));
                    } else {
                        try {
                            setFieldFromCursor(this, Track.class.getDeclaredField(key), cursor,
                                    key);
                        } catch (SecurityException e) {
                            Log.e(TAG, "error", e);
                        } catch (NoSuchFieldException e) {
                            Log.e(TAG, "error", e);
                        }
                    }
                }
                if (!user_played) {
                    this.updateUserPlayedFromDb(resolver, currentUserId);
                }
            }
            cursor.close();
        }
        if (user != null) {
            user.updateFromDb(resolver, currentUserId);
        }
    }

    public boolean updateUserPlayedFromDb(ContentResolver resolver, User user) {
        return updateUserPlayedFromDb(resolver, user.id);
    }

    public boolean updateUserPlayedFromDb(ContentResolver contentResolver, long userId) {
        Cursor cursor = contentResolver.query(Content.TRACK_PLAYS, null, TrackPlays.TRACK_ID
                + "= ? AND " + TrackPlays.USER_ID + " = ?", new String[] {
                String.valueOf(id), String.valueOf(userId)
        }, null);

        if (cursor != null) {
            user_played = cursor.getCount() > 0;
            cursor.close();
        }
        return user_played;
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
        return !TextUtils.isEmpty(stream_url);
    }

    public ContentValues buildContentValues(){
        ContentValues cv = new ContentValues();
        cv.put(Tracks.ID, id);
        cv.put(Tracks.PERMALINK, permalink);
        // account for partial objects, don't overwrite local full objects
        if (title != null) cv.put(Tracks.TITLE, title);
        if (duration != 0) cv.put(Tracks.DURATION, duration);
        if (stream_url != null) cv.put(Tracks.STREAM_URL, stream_url);
        if (user_id != 0) cv.put(Tracks.USER_ID, user_id);
        if (created_at != null) cv.put(Tracks.CREATED_AT, created_at.getTime());
        if (tag_list != null) cv.put(Tracks.TAG_LIST, tag_list);
        if (track_type != null) cv.put(Tracks.TRACK_TYPE, track_type);
        if (permalink_url != null) cv.put(Tracks.PERMALINK_URL, permalink_url);
        if (artwork_url != null) cv.put(Tracks.ARTWORK_URL, artwork_url);
        if (waveform_url != null) cv.put(Tracks.WAVEFORM_URL, waveform_url);
        if (downloadable) cv.put(Tracks.DOWNLOADABLE, downloadable);
        if (download_url != null) cv.put(Tracks.DOWNLOAD_URL, download_url);
        if (streamable) cv.put(Tracks.STREAMABLE, streamable);
        if (sharing != null) cv.put(Tracks.SHARING, sharing);
        // app level, only add these 2 if they have been set, otherwise they
        // might overwrite valid db values
        if (user_favorite) cv.put(Tracks.USER_FAVORITE, user_favorite);
        if (filelength > 0) cv.put(Tracks.FILELENGTH, filelength);
        return cv;
    }

    public void fillTags(FlowLayout ll, final Context context){
        TextView txt;
        FlowLayout.LayoutParams flowLP = new FlowLayout.LayoutParams(10, 10);

        final LayoutInflater inflater = LayoutInflater.from(context);

        if (!TextUtils.isEmpty(genre)) {
            txt = ((TextView) inflater.inflate(R.layout.genre_text, null));
            txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, TracksByTag.class);
                        intent.putExtra("genre", genre);
                        context.startActivity(intent);
                    }
                });
            txt.setText(genre);
            ll.addView(txt, flowLP);
        }
        for (final String t : humanTags()) {
            if (!TextUtils.isEmpty(t)) {
                txt = ((TextView) inflater.inflate(R.layout.tag_text, null));
                txt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, TracksByTag.class);
                        intent.putExtra("tag", t);
                        context.startActivity(intent);
                    }
                });
                txt.setText(t);
                ll.addView(txt, flowLP);
            }
        }

    }

    public String trackInfo() {
        StringBuilder str = new StringBuilder(200);

        if (!TextUtils.isEmpty(description)) {
            str.append(description.replace("\n", "<br/>")).append("<br/><br/>");
        }

        if (!TextUtils.isEmpty(key_signature)) {
            str.append(key_signature).append("<br/>");
        }
        if (bpm != 0) {
            str.append(bpm).append("<br/>");
        }

        if (!TextUtils.isEmpty(formattedLicense())) {
            str.append("<br/>").append(formattedLicense()).append("<br/><br/>");
        }

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

    public boolean createCache() {
        if (getCache().exists()) {
            return true;
        } else {
            CloudUtils.mkdirs(getCache().getParentFile());
            try {
                return getCache().createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "error creating cache "+getCache(), e);
                return false;
            }
        }
    }

    public File getCache() {
        if (mCacheFile == null) {
            mCacheFile = new File(Consts.EXTERNAL_TRACK_CACHE_DIRECTORY, CloudUtils.md5(Long.toString(id)));
        }
        return mCacheFile;
    }

    public boolean isCached() {
        return filelength > 0 &&  getCache().length() >= filelength;
    }

    public boolean touchCache() {
        File cache = getCache();
        if (cache.exists())  {
            if (!cache.setLastModified(System.currentTimeMillis())) {
                Log.w(TAG, "error touching "+cache);
                return false;
            } else {
                return true;
            }
        } else return false;
    }

    public boolean deleteCache() {
        File cache = getCache();
        if (cache != null && cache.exists()) {
            if (!cache.delete()) {
                Log.w(TAG, "error deleting " + cache);
                return false;
            } else {
                return true;
            }
        } else return false;
    }

    public CharSequence getElapsedTime(Context c) {
        if (mElapsedTime == null){
            mElapsedTime = CloudUtils.getTimeElapsed(c.getResources(),created_at.getTime());
        }

        return mElapsedTime;
    }

    @Override
    public void resolve(SoundCloudApplication application) {
        updateUserPlayedFromDb(application.getContentResolver(), application.getCurrentUserId());
    }

    @Override
    public String toString() {
        return "Track{" +
                "title='" + title + '\'' +
                ", user=" + user +
                '}';
    }

    @Override
    public String pageTrack(String... paths) {
        StringBuilder sb = new StringBuilder();
        if (user != null && !TextUtils.isEmpty(user.permalink)) {
            sb.append("/").append(user.permalink).append("/");
        }
        sb.append(permalink);
        for (CharSequence p : paths) {
            sb.append("/").append(p);
        }
        return sb.toString();
    }

    public boolean hasAvatar() {
        return user != null && !TextUtils.isEmpty(user.avatar_url);
    }
}
