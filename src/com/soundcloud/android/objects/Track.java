
package com.soundcloud.android.objects;

import java.io.File;
import java.lang.reflect.Field;

import org.codehaus.jackson.annotate.JsonProperty;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.soundcloud.android.CloudUtils;

public class Track extends BaseObj implements Parcelable {

    private static final String TAG = "Track";

    public static final String DOWNLOAD_STATUS_NONE = "none";

    public static final String DOWNLOAD_STATUS_PENDING = "pending";

    public static final String DOWNLOAD_STATUS_DOWNLOADED = "downloaded";

    private Long id;

    private String artwork_url;

    private String attachments_uri;

    private String avatar_url;

    private Float bpm;

    private Boolean commentable;

    private Integer comment_count;

    private String created_at;

    private CreatedWith created_with;

    private String description;

    private Boolean downloadable;

    private Integer download_count;

    private String download_url;

    private Integer downloads_remaining;

    private Integer duration;

    private String duration_formatted;

    private Integer favoritings_count;

    private String genre;

    private String isrc;

    private String key_signature;

    private User label;

    private String label_id;

    private String label_name;

    private String license;

    private String original_format;

    private String permalink;

    private String permalink_url;

    private String playback_count;

    private String purchase_url;

    private String release;

    private String release_day;

    private String release_month;

    private String release_year;

    private String secret_token;

    private String secret_uri;

    private Integer shared_to_count;

    private String sharing;

    private String state;

    private Boolean streamable;

    private String stream_url;

    private String tag_list;

    private String track_type;

    private String title;

    private String uri;

    private Boolean user_played;

    private String user_playback_count;

    private Boolean user_favorite;

    private Integer user_favorite_id;

    private User user;

    private Long user_id;

    private String video_url;

    private String waveform_url;

    public static class CreatedWith {
        private Integer id;

        private String name;

        private String uri;

        private String permalink_url;

        @JsonProperty("id")
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("uri")
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        @JsonProperty("permalink_url")
        public String getPermalinkUrl() {
            return permalink_url;
        }

        public void setPermalink_url(String permalink_url) {
            this.permalink_url = permalink_url;
        }
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("artwork_url")
    public String getArtworkUrl() {
        return artwork_url;
    }

    @JsonProperty("artwork_url")
    public void setArtworkUrl(String artwork_url) {
        this.artwork_url = artwork_url;
    }

    @JsonProperty("attachments_uri")
    public String getAttachmentsUri() {
        return attachments_uri;
    }

    @JsonProperty("attachments_uri")
    public void setAttachmentsUri(String attachments_uri) {
        this.attachments_uri = attachments_uri;
    }

    @JsonProperty("avatar_url")
    public String getAvatarUrl() {
        return avatar_url;
    }

    @JsonProperty("avatar_url")
    public void setAvatarUrl(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    @JsonProperty("bpm")
    public Float getBpm() {
        return bpm;
    }

    @JsonProperty("bpm")
    public void setBpm(Float bpm) {
        this.bpm = bpm;
    }

    @JsonProperty("commentable")
    public Boolean getCommentable() {
        return commentable;
    }

    @JsonProperty("commentable")
    public void setCommentable(Boolean commentable) {
        this.commentable = commentable;
    }

    @JsonProperty("comment_count")
    public Integer getCommentCount() {
        return comment_count;
    }

    @JsonProperty("comment_count")
    public void setCommentCount(Integer comment_count) {
        this.comment_count = comment_count;
    }

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return created_at;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }

    @JsonProperty("created_with")
    public CreatedWith getCreatedWith() {
        return created_with;
    }

    @JsonProperty("created_with")
    public void setCreatedWith(CreatedWith created_with) {
        this.created_with = created_with;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("downloadable")
    public Boolean getDownloadable() {
        return downloadable;
    }

    @JsonProperty("downloadable")
    public void setDownloadable(Boolean downloadable) {
        this.downloadable = downloadable;
    }

    @JsonProperty("download_count")
    public Integer getDownloadCount() {
        return download_count;
    }

    @JsonProperty("download_count")
    public void setDownloadCount(Integer download_count) {
        this.download_count = download_count;
    }

    @JsonProperty("download_url")
    public String getDownloadUrl() {
        return download_url;
    }

    @JsonProperty("download_url")
    public void setDownloadUrl(String download_url) {
        this.download_url = download_url;
    }

    @JsonProperty("downloads_remaining")
    public Integer getDownloadsRemaining() {
        return downloads_remaining;
    }

    @JsonProperty("downloads_remaining")
    public void setDownloadsRemaining(Integer downloads_remaining) {
        this.downloads_remaining = downloads_remaining;
    }

    @JsonProperty("duration")
    public Integer getDuration() {
        return duration;
    }

    @JsonProperty("duration")
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @JsonProperty("duration_formatted")
    public String getDurationFormatted() {
        return duration_formatted;
    }

    @JsonProperty("duration_formatted")
    public void setDurationFormatted(String duration_formatted) {
        this.duration_formatted = duration_formatted;
    }

    @JsonProperty("favoritings_count")
    public Integer getFavoritingsCount() {
        return favoritings_count;
    }

    @JsonProperty("favoritings_count")
    public void setFavoritingsCount(Integer favoritings_count) {
        this.favoritings_count = favoritings_count;
    }

    @JsonProperty("genre")
    public String getGenre() {
        return genre;
    }

    @JsonProperty("genre")
    public void setGenre(String genre) {
        this.genre = genre;
    }

    @JsonProperty("isrc")
    public String getIsrc() {
        return isrc;
    }

    @JsonProperty("isrc")
    public void setIsrc(String isrc) {
        this.isrc = isrc;
    }

    @JsonProperty("key_signature")
    public String getKeySignature() {
        return key_signature;
    }

    @JsonProperty("key_signature")
    public void setKeySignature(String key_signature) {
        this.key_signature = key_signature;
    }

    @JsonProperty("label")
    public User getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(User label) {
        this.label = label;
    }

    @JsonProperty("label_id")
    public String getLabelId() {
        return label_id;
    }

    @JsonProperty("label_id")
    public void setLabelId(String label_id) {
        this.label_id = label_id;
    }

    @JsonProperty("label_name")
    public String getLabelName() {
        return label_name;
    }

    @JsonProperty("label_name")
    public void setLabelName(String label_name) {
        this.label_name = label_name;
    }

    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    @JsonProperty("license")
    public void setLicense(String license) {
        this.license = license;
    }

    @JsonProperty("original_format")
    public String getOriginalFormat() {
        return original_format;
    }

    @JsonProperty("original_format")
    public void setOriginalFormat(String original_format) {
        this.original_format = original_format;
    }

    @JsonProperty("permalink")
    public String getPermalink() {
        return permalink;
    }

    @JsonProperty("permalink")
    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    @JsonProperty("permalink_url")
    public String getPermalinkUrl() {
        return permalink_url;
    }

    @JsonProperty("permalink_url")
    public void setPermalinkUrl(String permalink_url) {
        this.permalink_url = permalink_url;
    }

    @JsonProperty("playback_count")
    public String getPlaybackCount() {
        return playback_count;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(String playback_count) {
        this.playback_count = playback_count;
    }

    @JsonProperty("purchase_url")
    public String getPurchaseUrl() {
        return purchase_url;
    }

    @JsonProperty("purchase_url")
    public void setPurchaseUrl(String purchase_url) {
        this.purchase_url = purchase_url;
    }

    @JsonProperty("release")
    public String getRelease() {
        return release;
    }

    @JsonProperty("release")
    public void setRelease(String release) {
        this.release = release;
    }

    @JsonProperty("release_day")
    public String getReleaseDay() {
        return release_day;
    }

    @JsonProperty("release_day")
    public void setReleaseDay(String release_day) {
        this.release_day = release_day;
    }

    @JsonProperty("release_month")
    public String getReleaseMonth() {
        return release_month;
    }

    @JsonProperty("release_month")
    public void setReleaseMonth(String release_month) {
        this.release_month = release_month;
    }

    @JsonProperty("release_year")
    public String getReleaseYear() {
        return release_year;
    }

    @JsonProperty("release_year")
    public void setReleaseYear(String release_year) {
        this.release_year = release_year;
    }

    @JsonProperty("secret_token")
    public String getSecretToken() {
        return secret_token;
    }

    @JsonProperty("secret_token")
    public void setSecretToken(String secret_token) {
        this.secret_token = secret_token;
    }

    @JsonProperty("secret_uri")
    public String getSecretUri() {
        return secret_uri;
    }

    @JsonProperty("secret_uri")
    public void setSecretUri(String secret_uri) {
        this.secret_uri = secret_uri;
    }

    @JsonProperty("shared_to_count")
    public Integer getSharedToCount() {
        return shared_to_count;
    }

    @JsonProperty("shared_to_count")
    public void setSharedToCount(Integer shared_to_count) {
        this.shared_to_count = shared_to_count;
    }

    @JsonProperty("sharing")
    public String getSharing() {
        return sharing;
    }

    @JsonProperty("sharing")
    public void setSharing(String sharing) {
        this.sharing = sharing;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("streamable")
    public Boolean getStreamable() {
        return streamable;
    }

    @JsonProperty("streamable")
    public void setStreamable(Boolean streamable) {
        this.streamable = streamable;
    }

    @JsonProperty("stream_url")
    public String getStreamUrl() {
        return stream_url;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String stream_url) {
        this.stream_url = stream_url;
    }

    @JsonProperty("tag_list")
    public String getTagList() {
        return tag_list;
    }

    @JsonProperty("tag_list")
    public void setTagList(String tag_list) {
        this.tag_list = tag_list;
    }

    @JsonProperty("track_type")
    public String getTrackType() {
        return track_type;
    }

    @JsonProperty("track_type")
    public void setTrackType(String track_type) {
        this.track_type = track_type;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @JsonProperty("user_playback_count")
    public String getUserPlaybackCount() {
        return user_playback_count;
    }

    @JsonProperty("user_playback_count")
    public void setUserPlaybackCount(String user_playback_count) {
        this.user_playback_count = user_playback_count;
    }

    @JsonProperty("user_favorite")
    public Boolean getUserFavorite() {
        return user_favorite;
    }

    @JsonProperty("user_favorite")
    public void setUserFavorite(Boolean user_favorite) {
        this.user_favorite = user_favorite;
    }

    @JsonProperty("user")
    public User getUser() {
        return user;
    }

    @JsonProperty("user")
    public void setUser(User user) {
        this.user = user;
    }

    @JsonProperty("user_id")
    public Long getUserId() {
        return user_id;
    }

    @JsonProperty("user_id")
    public void setUserId(Long user_id) {
        this.user_id = user_id;
    }

    @JsonProperty("user_favorite_id")
    public Integer getUserFavoriteId() {
        return user_favorite_id;
    }

    @JsonProperty("user_favorite_id")
    public void setUserFavoriteId(Integer user_favorite_id) {
        this.user_favorite_id = user_favorite_id;
    }

    @JsonProperty("user_played")
    public Boolean getUserPlayed() {
        return user_played == null ? false : user_played;
    }

    @JsonProperty("user_played")
    public void setUserPlayed(Boolean user_played) {
        this.user_played = user_played;
    }

    @JsonProperty("video_url")
    public String getVideoUrl() {
        return video_url;
    }

    @JsonProperty("video_url")
    public void setVideoUrl(String video_url) {
        this.video_url = video_url;
    }

    @JsonProperty("waveform_url")
    public String getWaveformUrl() {
        return waveform_url;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveform_url) {
        this.waveform_url = waveform_url;
    }

    @JsonProperty("filelength")
    public Long getFilelength() {
        return filelength;
    }

    @JsonProperty("filelength")
    public void setFilelength(Long fileLength) {
        this.filelength = fileLength;
    }

    public Comment[] getComments() {
        return comments;
    }

    public void setComments(Comment[] comments) {
        this.comments = comments;
    }

    public static final String key_id = "id";

    public static final String key_uri = "uri";

    public static final String key_title = "title";

    // public static final String key_comments = "comments";

    public static final String key_favorited_by = "favorited_by";

    public static final String key_type = "type";

    public static final String key_tracklist = "tracklist";

    public static final String key_label_id = "label_id";

    public static final String key_release_day = "release_day";

    public static final String key_release_year = "release_year";

    public static final String key_created_at = "created_at";

    public static final String key_license = "license";

    public static final String key_label_name = "label_name";

    public static final String key_original_format = "original_format";

    public static final String key_track_type = "track_type";

    public static final String key_comment_count = "comment_count";

    public static final String key_favoritings_count = "favoritings_count";

    public static final String key_user_playback_count = "user_playback_count";

    public static final String key_purchase_url = "purchase_url";

    public static final String key_bpm = "bpm";

    public static final String key_artwork_url = "artwork_url";

    public static final String key_release_month = "release_month";

    public static final String key_release = "release";

    public static final String key_duration = "duration";

    public static final String key_duration_formatted = "duration_formatted";

    public static final String key_user_id = "user_id";

    public static final String key_user = "user";

    public static final String key_username = "username";

    public static final String key_user_permalink = "user_permalink";

    public static final String key_user_avatar_url = "avatar_url";

    public static final String key_user_favorite = "user_favorite";

    public static final String key_user_favorite_id = "user_favorite_id";

    public static final String key_waveform_url = "waveform_url";

    public static final String key_download_count = "download_count";

    public static final String key_downloadable = "downloadable";

    public static final String key_download_url = "download_url";

    public static final String key_download_status = "download_status";

    public static final String key_download_error = "download_error";

    public static final String key_user_played = "user_played";

    public static final String key_sharing = "sharing";

    public static final String key_state = "state";

    public static final String key_streamable = "streamable";

    public static final String key_stream_url = "stream_url";

    public static final String key_play_url = "play_url";

    public static final String key_local_play_url = "local_play_url";

    public static final String key_local_artwork_url = "local_artwork_url";

    public static final String key_local_waveform_url = "local_waveform_url";

    public static final String key_local_avatar_url = "local_avatar_url";

    public static final String key_permalink = "permalink";

    public static final String key_permalink_url = "permalink_url";

    public static final String key_video_url = "video_url";

    public static final String key_tag_list = "tag_list";

    public static final String key_isrc = "isrc";

    public static final String key_genre = "genre";

    public static final String key_key_signature = "key_signature";

    public static final String key_description = "description";

    public static final String key_load_progress = "progress";

    public static final String key_kb_loaded = "kb_loaded";

    public static final String key_kb_total = "kb_total";

    public static final String key_playback_count = "playback_count";

    public Comment[] comments;

    private boolean mIsPlaylist = false;

    private File mCacheFile;

    private Long filelength;

    private String mSignedUri;

    public enum Parcelables {
        track, user, comment
    }

    public void resolveData() {

        if (!mIsPlaylist) {
            if (CloudUtils.stringNullEmptyCheck(data.getString(Track.key_duration), true)) {
                data.putString(Track.key_duration_formatted, "0");
                return;
            }

            Integer duration = Integer.parseInt(data.getString(Track.key_duration));
            String durationStr = "";
            if (Math.floor(Math.floor((duration / 1000) / 60) / 60) > 0)
                durationStr = String.valueOf((int) Math
                        .floor(Math.floor((duration / 1000) / 60) / 60)
                        + "." + (int) Math.floor((duration / 1000) / 60) % 60)
                        + "." + String.format("%02d", (duration / 1000) % 60);
            else
                durationStr = String.valueOf((int) Math.floor((duration / 1000) / 60) % 60) + "."
                        + String.format("%02d", (duration / 1000) % 60);

            this.setDurationFormatted(durationStr);
        }

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
                try {
                    Field f = this.getPrivateField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) == 0 ? false
                                    : true);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
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
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isPlaylist() {
        return mIsPlaylist;
    }

    public File getCacheFile() {
        return mCacheFile;
    }

    public void setCacheFile(File cacheFile) {
        mCacheFile = cacheFile;
    }

    public String getSignedUri() {
        return mSignedUri;
    }

    public void setSignedUri(String signedUri) {
        mSignedUri = signedUri;
    }

}
