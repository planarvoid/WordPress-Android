package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackStats;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.LoadCommentsTask;
import com.soundcloud.android.storage.ResolverHelper;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

@Deprecated
@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicApiTrack extends Playable implements TrackRecord {
    public static final String EXTRA = "track";
    public static final String EXTRA_ID = "track_id";
    public static final Parcelable.Creator<PublicApiTrack> CREATOR = new Parcelable.Creator<PublicApiTrack>() {
        public PublicApiTrack createFromParcel(Parcel in) {
            return new PublicApiTrack(in);
        }

        public PublicApiTrack[] newArray(int size) {
            return new PublicApiTrack[size];
        }
    };
    private static final String TAG = "Track";
    private static final String API_MONETIZABLE_VALUE = "monetize";
    // API fields
    @JsonView(Views.Full.class) @Nullable public String policy;
    @JsonView(Views.Full.class) @Nullable public State state;
    @JsonView(Views.Full.class) public boolean commentable;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL) public PublicApiUser label;
    @JsonView(Views.Full.class) public String isrc;
    @JsonView(Views.Full.class) public String video_url;
    @JsonView(Views.Full.class) public String track_type;
    @JsonView(Views.Full.class) public String key_signature;
    @JsonView(Views.Full.class) public float bpm;
    @JsonView(Views.Full.class) public int playback_count = NOT_SET;
    @JsonView(Views.Full.class) public int download_count = NOT_SET;
    @JsonView(Views.Full.class) public int comment_count = NOT_SET;
    @JsonView(Views.Full.class) public String original_format;
    @JsonView(Views.Mini.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String user_uri;
    @JsonView(Views.Full.class)
    public String waveform_url;
    @JsonView(Views.Mini.class) public String stream_url;
    @JsonView(Views.Full.class) public int user_playback_count = NOT_SET;
    @JsonView(Views.Full.class)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public CreatedWith created_with;
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
    @JsonIgnore public List<PublicApiComment> comments;
    @JsonIgnore public int local_user_playback_count;
    @JsonIgnore public boolean local_cached;
    @JsonIgnore public LoadCommentsTask load_comments_task;
    @JsonIgnore public int last_playback_error = -1;

    public PublicApiTrack(ApiTrack suggestion) {
        setUrn(suggestion.getUrn().toString());
        setUser(new PublicApiUser(suggestion.getUser()));
        setTitle(suggestion.getTitle());
        setWaveformUrl(suggestion.getWaveformUrl());
        duration = suggestion.getDuration();
        artwork_url = suggestion.getArtworkUrl();
        genre = suggestion.getGenre();
        commentable = suggestion.isCommentable();
        stream_url = suggestion.getStreamUrl();
        tag_list = suggestion.getUserTags() == null ? ScTextUtils.EMPTY_STRING : TextUtils.join(" ", suggestion.getUserTags());
        created_at = suggestion.getCreatedAt();
        sharing = suggestion.getSharing();
        permalink_url = suggestion.getPermalinkUrl();
        policy = suggestion.getPolicy();

        final ApiTrackStats stats = suggestion.getStats();
        if (stats != null) {
            playback_count = stats.getPlaybackCount();
            likes_count = stats.getLikesCount();
            comment_count = stats.getCommentsCount();
            reposts_count = stats.getRepostsCount();
        }
    }

    public PublicApiTrack() {
        super();
    }

    public PublicApiTrack(long id) {
        super(id);
    }

    public PublicApiTrack(Parcel in) {
        Bundle b = in.readBundle(getClass().getClassLoader());
        super.readFromBundle(b);

        policy = b.getString("policy");
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
        attachments_uri = b.getString("attachments_uri");
        download_url = b.getString("download_url");
        downloads_remaining = b.getInt("downloads_remaining");
        secret_token = b.getString("secret_token");
        secret_uri = b.getString("secret_uri");

        local_user_playback_count = b.getInt("local_user_playback_count");
        local_cached = b.getBoolean("local_cached");
        last_playback_error = b.getInt("last_playback_error");
    }

    public PublicApiTrack(Cursor cursor) {
        super(cursor);
        policy = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.POLICIES_POLICY));
        state = State.fromString(cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.STATE)));
        track_type = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.TRACK_TYPE));

        waveform_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.WAVEFORM_URL));

        download_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.DOWNLOAD_URL));

        stream_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.STREAM_URL));
        playback_count = ResolverHelper.getIntOrNotSet(cursor, TableColumns.SoundView.PLAYBACK_COUNT);
        download_count = ResolverHelper.getIntOrNotSet(cursor, TableColumns.SoundView.DOWNLOAD_COUNT);
        comment_count = ResolverHelper.getIntOrNotSet(cursor, TableColumns.SoundView.COMMENT_COUNT);
        shared_to_count = ResolverHelper.getIntOrNotSet(cursor, TableColumns.SoundView.SHARED_TO_COUNT);
        commentable = cursor.getInt(cursor.getColumnIndex(TableColumns.SoundView.COMMENTABLE)) == 1;
        description = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.DESCRIPTION));

        final int localPlayCountIdx = cursor.getColumnIndex(TableColumns.SoundView.USER_PLAY_COUNT);
        if (localPlayCountIdx != -1) {
            local_user_playback_count = cursor.getInt(localPlayCountIdx);
        }
        final int cachedIdx = cursor.getColumnIndex(TableColumns.SoundView.CACHED);
        if (cachedIdx != -1) {
            local_cached = cursor.getInt(cachedIdx) == 1;
        }
    }

    public void setPolicy(String policy){
        this.policy = policy;
    }

    public String getPolicy(){
        return policy;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setStreamUrl(String streamUrl) {
        this.stream_url = streamUrl;
    }

    @Override
    public boolean isSyncable() {
        return false;
    }

    @Override
    public int getPlaybackCount() {
        return playback_count;
    }

    @Override
    public int getCommentsCount() {
        return comment_count;
    }

    @Override
    public int getLikesCount() {
        return likes_count;
    }

    @Override
    public int getRepostsCount() {
        return reposts_count;
    }

    @Override
    public void setId(long id) {
        super.setId(id);
        urn = Urn.forTrack(id);
    }

    @Nullable
    public String getGenreOrTag() {
        if (ScTextUtils.isNotBlank(genre)) {
            return genre;
        } else {
            List<String> tags = humanTags();
            if (!tags.isEmpty()) {
                return tags.get(0);
            } else {
                return null;
            }
        }
    }

    public Uri toUri() {
        return Content.TRACKS.forId(getId());
    }

    @JsonIgnore
    public boolean isWaitingOnState() {
        return state == null;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = super.getBundle();
        b.putString("policy", policy);
        b.putString("state", state != null ? state.value() : null);
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

    @Override
    public long getDuration() {
        return duration;
    }

    public String getWaveformUrl() {
        return waveform_url;
    }

    /**
     * GHETTO WAVEFORM FIX. Make the private API return something we can use and remove this
     */
    public void setWaveformUrl(String waveformUrl) {
        waveform_url = fixWaveform(waveformUrl);
    }

    public PropertySet toPropertySet() {
        final PropertySet track = super.toPropertySet()
                .put(TrackProperty.URN, Urn.forTrack(getId()))
                .put(TrackProperty.PLAY_COUNT, playback_count)
                .put(TrackProperty.COMMENTS_COUNT, comment_count)
                .put(TrackProperty.DESCRIPTION, description == null ? ScTextUtils.EMPTY_STRING : description);

        // we may not have policy in the db yet, as it was not always part of the public API
        if (policy != null){
            track.put(TrackProperty.POLICY, policy);
            track.put(TrackProperty.MONETIZABLE, isMonetizable());
        } else {
            track.put(TrackProperty.MONETIZABLE, false);
        }
        return track;
    }

    public boolean isMonetizable() {
        return policy != null && policy.equalsIgnoreCase(API_MONETIZABLE_VALUE);
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();


        if (stream_url != null) {
            cv.put(TableColumns.Sounds.STREAM_URL, stream_url);
        }
        if (state != null) {
            cv.put(TableColumns.Sounds.STATE, state.name);
        }
        if (track_type != null) {
            cv.put(TableColumns.Sounds.TRACK_TYPE, track_type);
        }
        if (waveform_url != null) {
            cv.put(TableColumns.Sounds.WAVEFORM_URL, waveform_url);
        }
        if (download_url != null) {
            cv.put(TableColumns.Sounds.DOWNLOAD_URL, download_url);
        }
        if (playback_count != NOT_SET) {
            cv.put(TableColumns.Sounds.PLAYBACK_COUNT, playback_count);
        }
        if (download_count != NOT_SET) {
            cv.put(TableColumns.Sounds.DOWNLOAD_COUNT, download_count);
        }
        if (comment_count != NOT_SET) {
            cv.put(TableColumns.Sounds.COMMENT_COUNT, comment_count);
        }
        if (commentable) {
            cv.put(TableColumns.Sounds.COMMENTABLE, commentable);
        }
        if (shared_to_count != NOT_SET) {
            cv.put(TableColumns.Sounds.SHARED_TO_COUNT, shared_to_count);
        }
        if (description != null) {
            cv.put(TableColumns.Sounds.DESCRIPTION, description);
        }
        if (isCompleteTrack()) {
            cv.put(TableColumns.Sounds.LAST_UPDATED, System.currentTimeMillis());
        }
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.TRACKS.uri;
    }

    public void setAppFields(PublicApiTrack t) {
        comments = t.comments;
    }

    public boolean isCompleteTrack() {
        return state != null && created_at != null && duration > 0;
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
            str.append(' ');
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
                    .append("</a><br/>");
        }

        return str.toString();
    }

    public String formattedLicense() {
        final StringBuilder sb = new StringBuilder(200);
        final String license = TextUtils.isEmpty(this.license) ? "all-rights-reserved" : this.license;
        if (license.startsWith("cc-")) {
            String cc = license.substring(3, license.length());

            sb.append("Licensed under a Creative Commons License (<a href='")
                    .append(getCCLink(cc))
                    .append("'>")
                    .append(cc.toUpperCase(Locale.US))
                    .append("</a>)");
        } else if ("no-rights-reserved".equals(license)) {
            sb.append("No Rights Reserved");
        }
        return sb.toString();
    }

    public int getEstimatedFileSize() {
        // 128kbps estimate
        return duration <= 0 ? 0 : (int) (((128 * duration) / 8) * 1024);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", getId())
                .add("title", title)
                .add("policy", policy)
                .add("permalink_url", permalink_url)
                .add("artwork_url", artwork_url)
                .add("duration", duration)
                .add("state", state)
                .add("user", user)
                .toString();
    }

    public
    @Nullable
    URL getWaveformDataURL() {
        if (TextUtils.isEmpty(waveform_url)) {
            return null;
        } else {
            try {
                Uri waveform = Uri.parse(waveform_url);
                return new URL("http://wis.sndcdn.com/" + waveform.getLastPathSegment());
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    public boolean hasWaveform() {
        return !TextUtils.isEmpty(waveform_url);
    }

    @Override
    public Refreshable getRefreshableResource() {
        return this;
    }

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.TRACK;
    }

    public PublicApiTrack updateFrom(PublicApiTrack updatedItem, CacheUpdateMode cacheUpdateMode) {
        super.updateFrom(updatedItem, cacheUpdateMode);
        stream_url = updatedItem.stream_url;
        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            policy = updatedItem.policy;
            user_like = updatedItem.user_like;
            commentable = updatedItem.commentable;
            state = updatedItem.state;
            waveform_url = updatedItem.waveform_url;
            playback_count = updatedItem.playback_count;
            comment_count = updatedItem.comment_count;
            shared_to_count = updatedItem.shared_to_count;
        }
        return this;
    }

    public String userTrackPermalink() {
        if (permalink == null) {
            return null;
        }
        return (user != null ? TextUtils.isEmpty(user.permalink) ? "" : user.permalink + "/" : "") + permalink;
    }

    @Override
    public int getTypeId() {
        return DB_TYPE_TRACK;
    }

    public String getStreamUrl() {
        return stream_url;
    }

    @Override
    public String getPermalinkUrl() {
        return permalink_url;
    }

    @Override
    public String getGenre() {
        return genre;
    }

    @Override
    public boolean isCommentable() {
        return commentable;
    }

    public ApiTrack toApiMobileTrack() {
        ApiTrack apiTrack = new ApiTrack(getUrn());
        apiTrack.setCreatedAt(created_at);
        apiTrack.setArtworkUrl(artwork_url);
        apiTrack.setCommentable(commentable);
        apiTrack.setDuration(duration);
        apiTrack.setGenre(genre);
        apiTrack.setMonetizable(isMonetizable());
        apiTrack.setPermalinkUrl(permalink_url);
        apiTrack.setPolicy(policy);
        apiTrack.setSharing(sharing);
        apiTrack.setStreamUrl(stream_url);
        apiTrack.setSyncable(isSyncable());
        apiTrack.setUserTags(humanTags());
        apiTrack.setTitle(title);
        apiTrack.setWaveformUrl(waveform_url);
        apiTrack.setUser(getUser().toApiMobileUser());

        final TrackStats stats = new TrackStats();
        stats.setCommentsCount(comment_count);
        stats.setPlaybackCount(playback_count);
        stats.setLikesCount(likes_count);
        stats.setRepostsCount(reposts_count);
        apiTrack.setStats(stats);

        return apiTrack;
    }

    protected static String fixWaveform(String input) {
        if (input != null && !input.endsWith("_m.png")) {
            return input.replace(".png", "_m.png");
        } else {
            return input;
        }
    }

    private String getCCLink(String license) {
        return "http://creativecommons.org/licenses/" + license + "/3.0";
    }

    public enum State {
        UNDEFINED(""),
        FINISHED("finished"),
        FAILED("failed"),
        READY("ready"),
        PROCESSING("processing");

        private final String name;

        State(String name) {
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
                if (s.name.equalsIgnoreCase(str)) {
                    return s;
                }
            }
            return UNDEFINED;
        }

        public boolean isStreamable() {
            // TODO: we can probably get away without including UNDEFINED in a subsequent release,
            // as it will get updated lazily on first load
            return FINISHED == this || UNDEFINED == this;
        }

        public boolean isFailed() {
            return FAILED == this;
        }

        public boolean isProcessing() {
            return PROCESSING == this;
        }

        public boolean isFinished() {
            return FINISHED == this;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreatedWith implements Parcelable {

        public static final Parcelable.Creator<CreatedWith> CREATOR = new Parcelable.Creator<CreatedWith>() {
            public CreatedWith createFromParcel(Parcel in) {
                return new CreatedWith(in);
            }

            public CreatedWith[] newArray(int size) {
                return new CreatedWith[size];
            }
        };
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
}
