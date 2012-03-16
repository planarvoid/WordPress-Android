
package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.TracksByTag;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Tracks;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.FlowLayout;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends ScModel implements Origin, Playable, Refreshable {
    private static final String TAG = "Track";

    public static class TrackHolder extends CollectionHolder<Track> {}

    // API fields
    @JsonView(Views.Full.class) public Date created_at;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Full.class) public int duration;
    @JsonView(Views.Full.class) public boolean commentable;
    @JsonView(Views.Full.class) public String state;
    @JsonView(Views.Full.class) public String sharing;  //  public | private
    @JsonView(Views.Full.class) public String tag_list;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Full.class) public String description;
    @JsonView(Views.Full.class) public boolean streamable;
    @JsonView(Views.Full.class) public boolean downloadable;
    @JsonView(Views.Full.class) public String genre;
    @JsonView(Views.Full.class) public String release;
    @JsonView(Views.Full.class) public String purchase_url;
    @JsonView(Views.Full.class) public Integer label_id;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL) public User label;
    @JsonView(Views.Full.class) public String label_name;
    @JsonView(Views.Full.class) public String isrc;
    @JsonView(Views.Full.class) public String video_url;
    @JsonView(Views.Full.class) public String track_type;
    @JsonView(Views.Full.class) public String key_signature;
    @JsonView(Views.Full.class)
    public float bpm;

    @JsonView(Views.Full.class) public int playback_count;
    @JsonView(Views.Full.class) public int download_count;
    @JsonView(Views.Full.class) public int comment_count;
    @JsonView(Views.Full.class) public int favoritings_count;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
    public int shared_to_count;

    @JsonView(Views.Mini.class) public String title;

    @JsonView(Views.Full.class)
    public Integer release_year;
    @JsonView(Views.Full.class)
    public Integer release_month;
    @JsonView(Views.Full.class)
    public Integer release_day;

    @JsonView(Views.Full.class) public String original_format;
    @JsonView(Views.Full.class) public String license;

    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String user_uri;
    @JsonView(Views.Mini.class) public String permalink_url;
    @JsonView(Views.Mini.class)
    public String artwork_url;

    @JsonView(Views.Full.class) public String waveform_url;

    @JsonView(Views.Full.class) public User user;

    @JsonView(Views.Mini.class) public String stream_url;

    @JsonView(Views.Full.class) public int user_playback_count;
    @JsonView(Views.Full.class) public boolean user_favorite;

    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public CreatedWith created_with;

    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public TrackSharing.SharingNote sharing_note;

    @JsonView(Views.Full.class) public String attachments_uri;

    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String download_url;  // if track downloadable or current user = owner

    // only shown to owner of track
    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
    public int downloads_remaining;

    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String secret_token;
    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String secret_uri;
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)

    // Fields used by app
    @JsonIgnore public List<Comment> comments;
    @JsonIgnore public int local_user_playback_count;
    @JsonIgnore public boolean local_cached;
    @JsonIgnore public FetchModelTask<Track> load_info_task;
    @JsonIgnore public LoadCommentsTask load_comments_task;
    @JsonIgnore public boolean full_track_info_loaded;
    @JsonIgnore public int last_playback_error = -1;
    @JsonIgnore private CharSequence _elapsedTime;
    @JsonIgnore private String _list_artwork_uri;

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
        return Content.TRACKS.forId(id);
    }

    @Override @JsonIgnore
    public Track getTrack() {
        return this;
    }

    @Override
    public CharSequence getTimeSinceCreated(Context context) {
        if (_elapsedTime == null) refreshTimeSinceCreated(context);
        return _elapsedTime;
    }

    @Override
    public void refreshTimeSinceCreated(Context context) {
        _elapsedTime = CloudUtils.getTimeElapsed(context.getResources(),created_at.getTime());
    }

    public void refreshListArtworkUri(Context context) {
        final String iconUrl = getArtwork();
        _list_artwork_uri = TextUtils.isEmpty(iconUrl) ? null : Consts.GraphicSize.formatUriForList(context, iconUrl);
    }

    public String getListArtworkUrl(Context context){
        if (TextUtils.isEmpty(_list_artwork_uri)) refreshListArtworkUri(context);
        return _list_artwork_uri;
    }


    @Override @JsonIgnore
    public User getUser() {
        return user;
    }

    public boolean isPublic() {
        return "public".equals(sharing);
    }

    public String getArtwork() {
        if (shouldLoadIcon() || (user != null && user.shouldLoadIcon())) {
            return TextUtils.isEmpty(artwork_url) ? user.avatar_url : artwork_url;
        } else {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class CreatedWith {
        @JsonView(Views.Full.class) public long id;
        @JsonView(Views.Full.class) public String name;
        @JsonView(Views.Full.class) public String uri;
        @JsonView(Views.Full.class) public String permalink_url;
        @JsonView(Views.Full.class) public String external_url;

        public boolean isEmpty() {
            return TextUtils.isEmpty(name) || TextUtils.isEmpty(permalink_url);
        }
    }

    public Track() {
    }

    public Track(Parcel in) {
        readFromParcel(in);
    }

    public Track(TracklistItem tracklistItem) {
        updateFromTracklistItem(tracklistItem);
    }

    public Track(Cursor cursor) {
        updateFromCursor(cursor);
    }

    public Track updateFromCursor(Cursor cursor) {
        final int trackIdIdx = cursor.getColumnIndex(DBHelper.ActivityView.TRACK_ID);
        if (trackIdIdx == -1) {
            id = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackView._ID));
        } else {
            id = cursor.getLong(cursor.getColumnIndex(DBHelper.ActivityView.TRACK_ID));
        }
        permalink = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.PERMALINK));
        duration = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.DURATION));
        created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.TrackView.CREATED_AT)));
        tag_list = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.TAG_LIST));
        track_type = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.TRACK_TYPE));
        title = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.TITLE));
        permalink_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.PERMALINK_URL));
        artwork_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.ARTWORK_URL));
        waveform_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.WAVEFORM_URL));
        downloadable = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.DOWNLOADABLE)) == 1;
        download_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.DOWNLOAD_URL));
        streamable = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.STREAMABLE)) == 1;
        stream_url = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.STREAM_URL));
        sharing = cursor.getString(cursor.getColumnIndex(DBHelper.TrackView.SHARING));
        playback_count = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.PLAYBACK_COUNT));
        download_count = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.DOWNLOAD_COUNT));
        comment_count = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.COMMENT_COUNT));
        favoritings_count = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.FAVORITINGS_COUNT));
        shared_to_count = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.SHARED_TO_COUNT));
        user_id = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.USER_ID));
        commentable = cursor.getInt(cursor.getColumnIndex(DBHelper.TrackView.COMMENTABLE)) == 1;
        final int sharingNoteIdx = cursor.getColumnIndex(DBHelper.TrackView.SHARING_NOTE_TEXT);
        if (sharingNoteIdx != -1) {
            sharing_note = new TrackSharing.SharingNote();
            sharing_note.text = cursor.getString(sharingNoteIdx);
        }

        user = SoundCloudApplication.USER_CACHE.fromTrackView(cursor);
        // gets joined in
        final int favIdx = cursor.getColumnIndex(DBHelper.TrackView.USER_FAVORITE);
        if (favIdx != -1) {
            user_favorite = cursor.getInt(favIdx) == 1;
        }
        final int localPlayCountIdx = cursor.getColumnIndex(DBHelper.TrackView.USER_PLAY_COUNT);
        if (localPlayCountIdx != -1) {
            local_user_playback_count = cursor.getInt(localPlayCountIdx);
        }
        final int cachedIdx = cursor.getColumnIndex(DBHelper.TrackView.CACHED);
        if (cachedIdx != -1) {
            local_cached = cursor.getInt(cachedIdx) == 1;
        }
        return this;
    }

    @Override
    public void resolve(SoundCloudApplication application) {
        refreshTimeSinceCreated(application);
        refreshListArtworkUri(application);
    }

    public void setAppFields(Track t) {
        comments = t.comments;
    }

    @JsonIgnore public boolean isStreamable() {
        return !TextUtils.isEmpty(stream_url);
    }

    public ContentValues buildContentValues(){
        ContentValues cv = super.buildContentValues();

        cv.put(Tracks.PERMALINK, permalink);
        // account for partial objects, don't overwrite local full objects
        if (title != null) cv.put(Tracks.TITLE, title);
        if (duration != 0) cv.put(Tracks.DURATION, duration);
        if (stream_url != null) cv.put(Tracks.STREAM_URL, stream_url);
        if (user_id != 0) {
            cv.put(Tracks.USER_ID, user_id);
        }  else if (user != null && user.isSaved()) {
            cv.put(Tracks.USER_ID, user.id);
        }

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
        if (playback_count != -1) cv.put(Tracks.PLAYBACK_COUNT, playback_count);
        if (download_count != -1) cv.put(Tracks.DOWNLOAD_COUNT, download_count);
        if (comment_count != -1) cv.put(Tracks.COMMENT_COUNT, comment_count);
        if (commentable) cv.put(Tracks.COMMENTABLE, commentable);
        if (favoritings_count != -1) cv.put(Tracks.FAVORITINGS_COUNT, favoritings_count);
        if (shared_to_count != -1) cv.put(Tracks.SHARED_TO_COUNT, shared_to_count);
        if (sharing_note != null && !sharing_note.isEmpty()) {
            cv.put(Tracks.SHARING_NOTE_TEXT, sharing_note.text);
        }
        if (isCompleteTrack()) {
            cv.put(Tracks.LAST_UPDATED, System.currentTimeMillis());
        }
        return cv;
    }

    private boolean isCompleteTrack(){
        return created_at != null && duration > 0;
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

    public String getUserName() {
        return user != null ? user.username : null;
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
            str.append(" ");
            if (Math.floor(bpm) == bpm) {
                str.append((int) bpm);
            } else {
                str.append(bpm);
            }
            str.append(" BPM <br/>");
        }

        if (!TextUtils.isEmpty(formattedLicense())) {
            str.append("<br/>").append(formattedLicense()).append("<br/><br/>");
        }

        if (!TextUtils.isEmpty(label_name)) {
            str.append("<b>Released By</b><br/>")
               .append(label_name).append("<br/>");

            if (release_day != null &&  release_day != 0) {
                str.append(release_year).append("<br/>");
            }
            str.append("<br />");
        }

        if (created_with != null && !created_with.isEmpty()) {
            str.append("Created with <a href=\"")
               .append(created_with.permalink_url)
               .append("\">")
               .append(created_with.name)
               .append("</a>")
               .append("<br/>");
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

    public int getEstimatedFileSize() {
        // 128kbps estimate
        return duration <= 0 ? 0 : ((128 * duration) / 8) * 1024;
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
    public String toString() {
        return "Track{" +
                "title='" + title + '\'' +
                ", user=" + user +
                '}';
    }

    public boolean hasAvatar() {
        return user != null && !TextUtils.isEmpty(user.avatar_url);
    }

    public String getAvatarUrl() {
        return user == null ? null : user.avatar_url;
    }

    @Override
    public long getRefreshableId() {
        return id;
    }

    @Override
    public ScModel getRefreshableResource() {
        return this;
    }

    @Override
    public boolean isStale(){
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.track;
    }

    public Intent getShareIntent() {
        if (!"public".equals(sharing)) return null;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT,
                title +
                (user != null ? " by " + user.username : "") + " on SoundCloud");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, permalink_url);

        return intent;
    }

    public Track updateFrom(Context c, ScModel updatedItem) {
         if (updatedItem instanceof TracklistItem){
             updateFromTracklistItem((TracklistItem) updatedItem);
         }
        refreshListArtworkUri(c);
        refreshTimeSinceCreated(c);
        return this;
    }

    public Track updateFromTracklistItem(TracklistItem tracklistItem) {
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
        shared_to_count = tracklistItem.shared_to_count;
        return this;
    }

    public boolean shouldLoadIcon() {
        return ImageUtils.checkIconShouldLoad(artwork_url);
    }

    public String userTrackPermalink() {
        if (permalink == null) return null;
        return (user != null ? TextUtils.isEmpty(user.permalink) ? "" : user.permalink+"/" : "") + permalink;
    }

    public static Track fromIntent(Intent intent, ContentResolver resolver) {
        if (intent == null) throw new IllegalArgumentException("intent is null");
        Track t = intent.getParcelableExtra("track");
        if (t == null) {
            long id = intent.getLongExtra("track_id", 0);
            // TODO: should be one operation
            t = SoundCloudApplication.TRACK_CACHE.get(id);
            if (t == null && resolver != null) {
              t = SoundCloudDB.getTrackById(resolver, id);
            }
            if (t != null) {
                SoundCloudApplication.TRACK_CACHE.put(t);
            } else {
                throw new IllegalArgumentException("Could not obtain track from intent "+intent);
            }
        }
        return t;
    }

    public static Uri getClientUri(long id) {
        return Uri.parse("soundcloud:tracks:"+id);
    }
}
