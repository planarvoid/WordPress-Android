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
    private List<String>    mTagList;
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
        this.mTagList = new ArrayList<String>();
        in.readStringList(this.mTagList);
        this.mCreatedAt = (Date) in.readSerializable();

    }

    public ExploreTracksSuggestion(String urn) {
        super(urn);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(String genre) {
        this.mGenre = genre;
    }

    public UserSummary getUser() {
        return mUser;
    }

    public String getUserName(){
        // TODO, remove this once we figure out the embedded problem
        return mUser != null ? mUser.getUsername() : "";
    }


    public void setUser(UserSummary user) {
        this.mUser = user;
    }

    public boolean isCommentable() {
        return mCommentable;
    }

    public void setCommentable(boolean commentable) {
        this.mCommentable = commentable;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String streamUrl) {
        this.mStreamUrl = streamUrl;
    }

    public String getWaveformUrl() {
        return mWaveformUrl;
    }

    @JsonProperty("playback_count")
    public void setPlaybackCount(long playback_count) {
        this.mPlaybackCount = playback_count;
    }

    public long getPlaybackCount() {
        return mPlaybackCount;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveformUrl) {
        this.mWaveformUrl = waveformUrl;
    }

    public List<String> getTagList() {
        return mTagList;
    }

    @JsonProperty("tag_list")
    public void setTagList(List<String> tagList) {
        this.mTagList = tagList;
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.mCreatedAt = createdAt;
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
        dest.writeStringList(this.mTagList);
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

    public String getArtworkUrl() {
        // todo, do we really want to hardcode this to this size??
        return getUrn().imageUri(ImageSize.T500).toString();
    }
}