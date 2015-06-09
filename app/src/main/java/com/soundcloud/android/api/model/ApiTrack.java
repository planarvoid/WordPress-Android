package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.legacy.model.TrackStats;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.propeller.PropertySet;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApiTrack extends ScModel implements PropertySetSource, TrackRecord {

    public static Creator<ApiTrack> CREATOR = new Creator<ApiTrack>() {
        public ApiTrack createFromParcel(Parcel source) {
            return new ApiTrack(source);
        }

        public ApiTrack[] newArray(int size) {
            return new ApiTrack[size];
        }
    };
    private String title;
    private String genre;
    private ApiUser user;
    private boolean commentable;
    private long duration = Consts.NOT_SET;
    private String streamUrl;
    private String waveformUrl;
    private List<String> userTags;
    private Date createdAt;
    private String artworkUrl;
    private String permalinkUrl;
    private Sharing sharing = Sharing.UNDEFINED;
    private TrackStats stats;

    private boolean monetizable;
    private String policy;
    private boolean syncable;

    public ApiTrack() { /* for Deserialization */ }

    public ApiTrack(Parcel in) {
        super(in);
        this.title = in.readString();
        this.genre = in.readString();
        this.user = in.readParcelable(ApiUser.class.getClassLoader());
        this.commentable = in.readByte() != 0;
        this.duration = in.readLong();
        this.streamUrl = in.readString();
        this.waveformUrl = in.readString();
        this.artworkUrl = in.readString();
        this.userTags = new ArrayList<>();
        in.readStringList(this.userTags);
        this.createdAt = (Date) in.readSerializable();
        this.permalinkUrl = in.readString();
        this.monetizable = in.readByte() != 0;
        this.policy = in.readString();
        this.syncable = in.readByte() != 0;
    }

    public ApiTrack(String urnString) {
        super(urnString);
    }

    public ApiTrack(Urn urn) {
        super(urn);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public UserRecord getUser() {
        return user;
    }

    public void setUser(ApiUser user) {
        this.user = user;
    }

    public String getUserName() {
        return user != null ? user.getUsername() : "";
    }

    public boolean isCommentable() {
        return commentable;
    }

    public void setCommentable(boolean commentable) {
        this.commentable = commentable;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getWaveformUrl() {
        return waveformUrl;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveformUrl) {
        this.waveformUrl = waveformUrl;
    }

    @Deprecated
    public String getArtworkUrl() {
        return artworkUrl;
    }

    @JsonProperty("artwork_url")
    public void setArtworkUrl(String mArtworkUrl) {
        this.artworkUrl = mArtworkUrl;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    @JsonProperty("permalink_url")
    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }

    public TrackStats getStats() {
        return stats;
    }

    public void setStats(TrackStats stats) {
        this.stats = stats;
    }

    public List<String> getUserTags() {
        return userTags;
    }

    @JsonProperty("user_tags")
    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Sharing getSharing() {
        return sharing;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public boolean isMonetizable() {
        return monetizable;
    }

    @JsonProperty("monetizable")
    public void setMonetizable(boolean monetizable) {
        this.monetizable = monetizable;
    }

    public String getPolicy() {
        return policy;
    }

    @JsonProperty("policy")
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public boolean isSyncable() {
        return syncable;
    }

    @Override
    public int getPlaybackCount() {
        return stats.getPlaybackCount();
    }

    @Override
    public int getCommentsCount() {
        return stats.getCommentsCount();
    }

    @Override
    public int getLikesCount() {
        return stats.getLikesCount();
    }

    @Override
    public int getRepostsCount() {
        return stats.getRepostsCount();
    }

    @JsonProperty("syncable")
    public void setSyncable(boolean syncable) {
        this.syncable = syncable;
    }

    @JsonProperty("_embedded")
    public void setRelatedResources(RelatedResources relatedResources) {
        this.user = relatedResources.user;
        this.stats = relatedResources.stats;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.title);
        dest.writeString(this.genre);
        dest.writeParcelable(this.user, flags);
        dest.writeByte(this.commentable ? (byte) 1 : (byte) 0);
        dest.writeLong(this.duration);
        dest.writeString(this.streamUrl);
        dest.writeString(this.waveformUrl);
        dest.writeString(this.artworkUrl);
        dest.writeStringList(this.userTags);
        dest.writeSerializable(this.createdAt);
        dest.writeString(this.permalinkUrl);
        dest.writeByte(this.monetizable ? (byte) 1 : (byte) 0);
        dest.writeString(this.policy);
        dest.writeByte(this.syncable ? (byte) 1 : (byte) 0);
    }

    public Boolean isPrivate() {
        return getSharing() != Sharing.PUBLIC;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("title", title)
                .add("genre", genre)
                .add("user", user)
                .add("commentable", commentable)
                .add("duration", duration)
                .add("streamUrl", streamUrl)
                .add("waveformUrl", waveformUrl)
                .add("userTags", userTags)
                .add("createdAt", createdAt)
                .add("artworkUrl", artworkUrl)
                .add("permalinkUrl", permalinkUrl)
                .add("monetizable", monetizable)
                .add("syncable", syncable)
                .add("policy", policy)
                .add("sharing", sharing)
                .add("stats", stats).toString();
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                TrackProperty.URN.bind(getUrn()),
                TrackProperty.TITLE.bind(getTitle()),
                TrackProperty.CREATED_AT.bind(getCreatedAt()),
                TrackProperty.DURATION.bind(getDuration()),
                TrackProperty.IS_PRIVATE.bind(isPrivate()),
                TrackProperty.WAVEFORM_URL.bind(getWaveformUrl()),
                TrackProperty.PERMALINK_URL.bind(getPermalinkUrl()),
                TrackProperty.MONETIZABLE.bind(isMonetizable()),
                TrackProperty.SYNCABLE.bind(isSyncable()),
                TrackProperty.POLICY.bind(getPolicy()),
                TrackProperty.PLAY_COUNT.bind(getStats().getPlaybackCount()),
                TrackProperty.COMMENTS_COUNT.bind(getStats().getCommentsCount()),
                TrackProperty.LIKES_COUNT.bind(getStats().getLikesCount()),
                TrackProperty.REPOSTS_COUNT.bind(getStats().getRepostsCount()),
                TrackProperty.CREATOR_NAME.bind(getUserName()),
                TrackProperty.CREATOR_URN.bind(getUser() != null ? getUser().getUrn() : Urn.NOT_SET),
                TrackProperty.GENRE.bind(Optional.fromNullable(genre))
        );
    }

    private static class RelatedResources {
        private ApiUser user;
        private TrackStats stats;

        void setUser(ApiUser user) {
            this.user = user;
        }

        void setStats(TrackStats stats) {
            this.stats = stats;
        }
    }
}
