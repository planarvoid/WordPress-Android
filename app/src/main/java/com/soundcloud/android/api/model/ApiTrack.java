package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.legacy.model.TrackStats;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ApiTrack extends ScModel implements PropertySetSource {

    private String title;
    private String genre;
    private ApiUser user;
    private boolean commentable;
    private int duration = Consts.NOT_SET;
    private String streamUrl;
    private String waveformUrl;
    private List<String> userTags;
    private Date createdAt;
    private String artworkUrl;
    private String permalinkUrl;
    private boolean monetizable;
    private String policy;
    private Sharing sharing = Sharing.UNDEFINED;
    private TrackStats stats;

    public ApiTrack() { /* for Deserialization */ }

    public ApiTrack(Parcel in) {
        super(in);
        this.title = in.readString();
        this.genre = in.readString();
        this.user = in.readParcelable(ApiUser.class.getClassLoader());
        this.commentable = in.readByte() != 0;
        this.duration = in.readInt();
        this.streamUrl = in.readString();
        this.waveformUrl = in.readString();
        this.artworkUrl = in.readString();
        this.userTags = new ArrayList<String>();
        in.readStringList(this.userTags);
        this.createdAt = (Date) in.readSerializable();
        this.permalinkUrl = in.readString();
        this.monetizable = in.readByte() != 0;
        this.policy = in.readString();
    }

    public ApiTrack(String urn) {
        super(urn);
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public ApiUser getUser() {
        return user;
    }

    public String getUserName() {
        return user != null ? user.getUsername() : "";
    }

    public boolean isCommentable() {
        return commentable;
    }

    public int getDuration() {
        return duration;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getWaveformUrl() {
        return waveformUrl;
    }

    @Deprecated
    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    public TrackStats getStats() {
        return stats;
    }

    public List<String> getUserTags() {
        return userTags;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Sharing getSharing() {
        return sharing;
    }

    public boolean isMonetizable() {
        return monetizable;
    }

    public String getPolicy() {
        return policy;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStats(TrackStats stats) {
        this.stats = stats;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setUser(ApiUser user) {
        this.user = user;
    }

    public void setCommentable(boolean commentable) {
        this.commentable = commentable;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveformUrl) {
        this.waveformUrl = waveformUrl;
    }

    @JsonProperty("permalink_url")
    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }

    @JsonProperty("user_tags")
    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    @JsonProperty("artwork_url")
    public void setArtworkUrl(String mArtworkUrl) {
        this.artworkUrl = mArtworkUrl;
    }

    @JsonProperty("monetizable")
    public void setMonetizable(boolean monetizable) {
        this.monetizable = monetizable;
    }

    @JsonProperty("policy")
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        dest.writeInt(this.duration);
        dest.writeString(this.streamUrl);
        dest.writeString(this.waveformUrl);
        dest.writeString(this.artworkUrl);
        dest.writeStringList(this.userTags);
        dest.writeSerializable(this.createdAt);
        dest.writeString(this.permalinkUrl);
        dest.writeByte(this.monetizable ? (byte) 1 : (byte) 0);
        dest.writeString(this.policy);
    }

    public static Creator<ApiTrack> CREATOR = new Creator<ApiTrack>() {
        public ApiTrack createFromParcel(Parcel source) {
            return new ApiTrack(source);
        }

        public ApiTrack[] newArray(int size) {
            return new ApiTrack[size];
        }
    };

    public Boolean isPrivate() {
        return getSharing() != Sharing.PUBLIC;
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

    @Override
    public String toString() {
        return "ApiTrack{" +
                "title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", user=" + user +
                ", commentable=" + commentable +
                ", duration=" + duration +
                ", streamUrl='" + streamUrl + '\'' +
                ", waveformUrl='" + waveformUrl + '\'' +
                ", userTags=" + userTags +
                ", createdAt=" + createdAt +
                ", artworkUrl='" + artworkUrl + '\'' +
                ", permalinkUrl='" + permalinkUrl + '\'' +
                ", monetizable=" + monetizable +
                ", policy='" + policy + '\'' +
                ", sharing=" + sharing +
                ", stats=" + stats +
                '}';
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
                TrackProperty.POLICY.bind(getPolicy()),
                TrackProperty.PLAY_COUNT.bind(getStats().getPlaybackCount()),
                TrackProperty.COMMENTS_COUNT.bind(getStats().getCommentsCount()),
                TrackProperty.LIKES_COUNT.bind(getStats().getLikesCount()),
                TrackProperty.REPOSTS_COUNT.bind(getStats().getRepostsCount()),
                TrackProperty.CREATOR_NAME.bind(getUserName()),
                TrackProperty.CREATOR_URN.bind(getUser() != null ? getUser().getUrn() : Urn.NOT_SET)
        );
    }
}