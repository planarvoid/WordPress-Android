
package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.track.TracksByTag;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Sounds;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.FlowLayout;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends Sound implements Playable {
    private static final String TAG = "Track";
    public static final String EXTRA = "track";

    // API fields
    @JsonView(Views.Full.class) @Nullable public State state;
    @JsonView(Views.Full.class) public boolean commentable;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL) public User label;
    @JsonView(Views.Full.class) public String isrc;
    @JsonView(Views.Full.class) public String video_url;
    @JsonView(Views.Full.class) public String track_type;
    @JsonView(Views.Full.class) public String key_signature;
    @JsonView(Views.Full.class) public float bpm;

    @JsonView(Views.Full.class) public int playback_count;
    @JsonView(Views.Full.class) public int download_count;
    @JsonView(Views.Full.class) public int comment_count;
    @JsonView(Views.Full.class) public int reposts_count;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
    public int shared_to_count;

    @JsonView(Views.Full.class) public String original_format;

    @JsonView(Views.Mini.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String user_uri;

    @JsonView(Views.Full.class) public String waveform_url;


    @JsonView(Views.Mini.class) public String stream_url;

    @JsonView(Views.Full.class) public int user_playback_count;


    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public CreatedWith created_with;

    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public SharingNote sharing_note;

    @JsonView(Views.Full.class) public String attachments_uri;

    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String download_url;  // if track downloadable or current user = owner

    // only shown to owner of track
    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT) public int downloads_remaining;

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
    @JsonIgnore public int last_playback_error = -1;

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
    public Track getSound() {
        return this;
    }

    public Track getTrack() {
        return this;
    }

    @Override
    public Date getCreatedAt() {
        return created_at;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }



    @JsonIgnore
    public boolean isStreamable() {
        return !TextUtils.isEmpty(stream_url) && (state == null || state.isStreamable());
    }

    public boolean isProcessing() {
        return state != null && state.isProcessing();
    }

    public boolean isFinished() {
        return state != null && state.isFinished();
    }

    public boolean isFailed() {
        return state != null && state.isFailed();
    }

    public boolean isPublic() {
        return sharing == null || sharing.isPublic();
    }

    public String getArtwork() {
        if (shouldLoadIcon() || (user != null && user.shouldLoadIcon())) {
            return TextUtils.isEmpty(artwork_url) ? user.avatar_url : artwork_url;
        } else {
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = super.getBundle();
        b.putString("state", state.value());
        b.putBoolean("commentable", commentable);
        b.putParcelable("label", label);
        b.putString("isrc", isrc);
        b.putString("video_url", video_url);
        b.putString("track_type", track_type);
        b.putString("key_signature", key_signature);
        b.putFloat("bpm", bpm);
        b.putInt("playback_count", playback_count);
        b.putInt("download_count", download_count);
        b.putInt("comment_count", comment_count);
        b.putInt("reposts_count", reposts_count);
        b.putInt("shared_to_count", shared_to_count);
        b.putString("original_format", original_format);
        b.putString("user_uri", user_uri);
        b.putString("waveform_url", waveform_url);
        b.putString("stream_url", stream_url);
        b.putInt("user_playback_count", user_playback_count);
        b.putBoolean("user_like", user_like);
        b.putBoolean("user_repost", user_repost);
        b.putParcelable("created_with", created_with);
        b.putParcelable("sharing_note", sharing_note);
        b.putString("attachments_uri", attachments_uri);
        b.putString("download_url", download_url);
        b.putInt("downloads_remaining", downloads_remaining);
        b.putString("secret_token", secret_token);
        b.putString("secret_uri", secret_uri);

        b.putInt("local_user_playback_count", local_user_playback_count);
        b.putBoolean("local_cached", local_cached);
        b.putInt("last_playback_error", last_playback_error);

        /* the following fields are left out because they are too expensive or complex
        public List<Comment> comments;
        public FetchModelTask<Track> load_info_task;
        public LoadCommentsTask load_comments_task;
        */

        dest.writeBundle(b);
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class CreatedWith implements Parcelable {
        @JsonView(Views.Full.class) public long id;
        @JsonView(Views.Full.class) public String name;
        @JsonView(Views.Full.class) public String uri;
        @JsonView(Views.Full.class) public String permalink_url;
        @JsonView(Views.Full.class) public String external_url;

        public CreatedWith() {
            super();
        }

        public CreatedWith(Parcel in) {
            id = in.readLong();
            name = in.readString();
            uri = in.readString();
            permalink_url = in.readString();
            external_url = in.readString();

        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(name) || TextUtils.isEmpty(permalink_url);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeString(name);
            dest.writeString(uri);
            dest.writeString(permalink_url);
            dest.writeString(external_url);
        }
    }

    public Track() {
        super();
    }

    public Track(Parcel in) {
        Bundle b = in.readBundle();
        super.readFromBundle(b);

        state = State.fromString(b.getString("state"));
        commentable = b.getBoolean("commentable");
        label = b.getParcelable("label");
        isrc = b.getString("isrc");
        video_url = b.getString("video_url");
        track_type = b.getString("track_type");
        key_signature = b.getString("key_signature");
        bpm = b.getFloat("bpm");
        playback_count = b.getInt("playback_count");
        download_count = b.getInt("download_count");
        comment_count = b.getInt("comment_count");
        reposts_count = b.getInt("reposts_count");
        shared_to_count = b.getInt("shared_to_count");
        original_format = b.getString("original_format");
        user_uri = b.getString("user_uri");
        waveform_url = b.getString("waveform_url");
        stream_url = b.getString("stream_url");
        user_playback_count = b.getInt("user_playback_count");
        user_like = b.getBoolean("user_like");
        user_repost = b.getBoolean("user_repost");
        created_with = b.getParcelable("created_with");
        sharing_note = b.getParcelable("sharing_note");
        attachments_uri = b.getString("attachments_uri");
        download_url = b.getString("download_url");
        downloads_remaining = b.getInt("downloads_remaining");
        secret_token = b.getString("secret_token");
        secret_uri = b.getString("secret_uri");

        local_user_playback_count = b.getInt("local_user_playback_count");
        local_cached = b.getBoolean("local_cached");
        last_playback_error = b.getInt("last_playback_error");
    }

    Track(Cursor cursor) {
        super(cursor);
        state = State.fromString(cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.STATE)));
        track_type = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.TRACK_TYPE));

        waveform_url = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.WAVEFORM_URL));

        download_url = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.DOWNLOAD_URL));

        stream_url = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.STREAM_URL));
        playback_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.PLAYBACK_COUNT));
        download_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.DOWNLOAD_COUNT));
        comment_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.COMMENT_COUNT));
        shared_to_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.SHARED_TO_COUNT));
        commentable = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.COMMENTABLE)) == 1;

        final int sharingNoteIdx = cursor.getColumnIndex(DBHelper.SoundView.SHARING_NOTE_TEXT);
        if (sharingNoteIdx != -1) {
            sharing_note = new SharingNote();
            sharing_note.text = cursor.getString(sharingNoteIdx);
        }


        final int localPlayCountIdx = cursor.getColumnIndex(DBHelper.SoundView.USER_PLAY_COUNT);
        if (localPlayCountIdx != -1) {
            local_user_playback_count = cursor.getInt(localPlayCountIdx);
        }
        final int cachedIdx = cursor.getColumnIndex(DBHelper.SoundView.CACHED);
        if (cachedIdx != -1) {
            local_cached = cursor.getInt(cachedIdx) == 1;
        }
    }

    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Sounds._TYPE, Sound.DB_TYPE_TRACK);

        if (stream_url != null) cv.put(DBHelper.Sounds.STREAM_URL, stream_url);
        if (state != null) cv.put(DBHelper.Sounds.STATE, state.name);
        if (track_type != null) cv.put(DBHelper.Sounds.TRACK_TYPE, track_type);
        if (waveform_url != null) cv.put(DBHelper.Sounds.WAVEFORM_URL, waveform_url);
        if (download_url != null) cv.put(DBHelper.Sounds.DOWNLOAD_URL, download_url);
        if (playback_count != -1) cv.put(DBHelper.Sounds.PLAYBACK_COUNT, playback_count);
        if (download_count != -1) cv.put(DBHelper.Sounds.DOWNLOAD_COUNT, download_count);
        if (comment_count != -1) cv.put(DBHelper.Sounds.COMMENT_COUNT, comment_count);
        if (commentable) cv.put(DBHelper.Sounds.COMMENTABLE, commentable);
        if (likes_count != -1) cv.put(Sounds.LIKES_COUNT, likes_count);
        if (shared_to_count != -1) cv.put(DBHelper.Sounds.SHARED_TO_COUNT, shared_to_count);
        if (sharing_note != null && !sharing_note.isEmpty()) {
            cv.put(DBHelper.Sounds.SHARING_NOTE_TEXT, sharing_note.text);
        }
        if (isCompleteTrack()) {
            cv.put(DBHelper.Sounds.LAST_UPDATED, System.currentTimeMillis());
        }
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.TRACKS.uri;
    }

    @Override
    public void resolve(Context context) {

        refreshTimeSinceCreated(context);
        refreshListArtworkUri(context);
    }

    public void setAppFields(Track t) {
        comments = t.comments;
    }

    private boolean isCompleteTrack(){
        return state != null && created_at != null && duration > 0;
    }

    public void fillTags(ViewGroup view, final Context context){
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
            view.addView(txt, flowLP);
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
                view.addView(txt, flowLP);
            }
        }
    }

    // TODO, THIS SUCKS
    public FetchModelTask<Track> refreshInfoAsync(AndroidCloudAPI api, FetchModelTask.FetchModelListener<Track> listener) {
        if (load_info_task == null){
            if (AndroidUtils.isTaskFinished(load_info_task)) {
                load_info_task = new FetchTrackTask(api, id);
            }
            load_info_task.addListener(listener);
            if (AndroidUtils.isTaskPending(load_info_task)) {
                load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, id));
            }
        }
        return load_info_task;
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
            if (FloatMath.floor(bpm) == bpm) {
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

            if (release_day > 0) {
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
                "id="+id+
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", state=" + state +
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
    public ScResource getRefreshableResource() {
        return this;
    }

    @Override
    public boolean isStale(){
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.track;
    }

    public Intent getShareIntent() {
        if (sharing == null || !sharing.isPublic()) return null;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT,
                title +
                (user != null ? " by " + user.username : "") + " on SoundCloud");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, permalink_url);

        return intent;
    }

    public Intent getPlayIntent() {
        return new Intent(CloudPlaybackService.PLAY_ACTION).putExtra(EXTRA, this);
    }

    public void setUpdated() {
        last_updated = System.currentTimeMillis();
    }

    public Track updateFrom(Track updatedItem, CacheUpdateMode cacheUpdateMode) {
        super.updateFrom(updatedItem,cacheUpdateMode);
        stream_url = updatedItem.stream_url;
        if (cacheUpdateMode == CacheUpdateMode.FULL){
            commentable = updatedItem.commentable;
            state = updatedItem.state;
            waveform_url = updatedItem.waveform_url;
            playback_count = updatedItem.playback_count;
            comment_count = updatedItem.comment_count;
            shared_to_count = updatedItem.shared_to_count;
        }
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
        Track t = intent.getParcelableExtra(EXTRA);
        if (t == null) {
            t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra("track_id", 0));
            if (t == null) {
                throw new IllegalArgumentException("Could not obtain track from intent "+intent);
            }
        }
        return t;
    }

    public static Uri getClientUri(long id) {
        return Uri.parse("soundcloud:tracks:"+id);
    }



    public static enum State {
        UNDEFINED(""),
        FINISHED("finished"),
        FAILED("failed"),
        READY("ready"),
        PROCESSING("processing");

        private final String name;
        private State(String name){
            this.name = name;
        }

        @JsonValue
        public String value() {
            return name;
        }

        // don't use built in valueOf to create so we can handle nulls and unknowns ourself
        @JsonCreator
        public static State fromString(String str) {
            for (State s : values()) {
                if (s.name.equalsIgnoreCase(str)) return s;
            }
            return UNDEFINED;
        }

        public boolean isStreamable(){
            // TODO: we can probably get away without including UNDEFINED in a subsequent release,
            // as it will get updated lazily on first load
            return FINISHED == this || UNDEFINED == this;
        }

        public boolean isFailed()     { return FAILED == this; }
        public boolean isProcessing() { return PROCESSING == this; }
        public boolean isFinished()   { return FINISHED == this; }
    }

}
