package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.utils.images.ImageSize;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExploreTracksSuggestion extends ScModel {

    private String          mTitle;
    private String          mGenre;
    private UserSummary     mUser;
    private boolean         mCommentable;
    private int             mDuration = NOT_SET;
    private String          mStreamUrl;
    private String          mWaveformUrl;
    private List<String>    mUserTags;
    private Date            mCreatedAt;
    private long            mPlaybackCount;

    public ExploreTracksSuggestion() { /* for Deserialization */ }

    public ExploreTracksSuggestion(Parcel in) {
        super(in);
        this.mTitle = in.readString();
        this.mGenre = in.readString();
        this.mUser = in.readParcelable(UserSummary.class.getClassLoader());
        this.mCommentable = in.readByte() != 0;
        this.mDuration = in.readInt();
        this.mStreamUrl = in.readString();
        this.mWaveformUrl = in.readString();
        this.mUserTags = new ArrayList<String>();
        in.readStringList(this.mUserTags);
        this.mCreatedAt = (Date) in.readSerializable();

    }

    public ExploreTracksSuggestion(String urn) {
        super(urn);
    }

    public String getTitle() {
        return mTitle;
    }

    public String getGenre() {
        return mGenre;
    }

    public UserSummary getUser() {
        return mUser;
    }

    public String getUserName(){
       return mUser != null ? mUser.getUsername() : "";
    }

    public boolean isCommentable() {
        return mCommentable;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    public String getWaveformUrl() {
        return mWaveformUrl;
    }

    public long getPlaybackCount() {
        return mPlaybackCount;
    }

    public List<String> getUserTags() {
        return mUserTags;
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public String getArtworkUrl() {
        return getUrn().imageUri(ImageSize.T500).toString();
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public void setGenre(String genre) {
        this.mGenre = genre;
    }

    public void setUser(UserSummary user) {
        this.mUser = user;
    }

    public void setCommentable(boolean commentable) {
        this.mCommentable = commentable;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String streamUrl) {
        this.mStreamUrl = streamUrl;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playback_count) {
        this.mPlaybackCount = playback_count;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveformUrl) {
        this.mWaveformUrl = waveformUrl;
    }

    @JsonProperty("user_tags")
    public void setUserTags(List<String> userTags) {
        this.mUserTags = userTags;
    }

    @JsonProperty("created_at")
    private void setCreatedAt(Date createdAt) {
        this.mCreatedAt = createdAt;
    }


    @JsonProperty("_embedded")
    public void setRelatedResources(RelatedResources relatedResources) {
        this.mUser = relatedResources.user;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.mTitle);
        dest.writeString(this.mGenre);
        dest.writeParcelable(this.mUser, flags);
        dest.writeByte(mCommentable ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mDuration);
        dest.writeString(this.mStreamUrl);
        dest.writeString(this.mWaveformUrl);
        dest.writeStringList(this.mUserTags);
        dest.writeSerializable(this.mCreatedAt);
    }

    public static Creator<ExploreTracksSuggestion> CREATOR = new Creator<ExploreTracksSuggestion>() {
        public ExploreTracksSuggestion createFromParcel(Parcel source) {
            return new ExploreTracksSuggestion(source);
        }

        public ExploreTracksSuggestion[] newArray(int size) {
            return new ExploreTracksSuggestion[size];
        }
    };

    private static class RelatedResources {
        private UserSummary user;

        void setUser(UserSummary user) {
            this.user = user;
        }
    }
}