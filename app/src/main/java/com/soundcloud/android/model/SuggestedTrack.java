package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SuggestedTrack extends ScModel {

    private String          mTitle;
    private String          mGenre;
    private MiniUser        mUser;
    private boolean         mCommentable;
    private int             mDuration = NOT_SET;
    private String          mStreamUrl;
    private String          mWaveformUrl;
    private List<String>    mTagList;
    private Date            mCreatedAt;

    public SuggestedTrack() { /* for Deserialization */ }

    public SuggestedTrack(Parcel in) {
        super(in);
        this.mTitle = in.readString();
        this.mGenre = in.readString();
        this.mUser = in.readParcelable(MiniUser.class.getClassLoader());
        this.mCommentable = in.readByte() != 0;
        this.mDuration = in.readInt();
        this.mStreamUrl = in.readString();
        this.mWaveformUrl = in.readString();
        this.mTagList = new ArrayList<String>();
        in.readStringList(this.mTagList);
        this.mCreatedAt = (Date) in.readSerializable();

    }

    public SuggestedTrack(String urn) {
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

    public MiniUser getUser() {
        return mUser;
    }

    public void setUser(MiniUser user) {
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

    public String getmWaveformUrl() {
        return mWaveformUrl;
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

    public static Creator<SuggestedTrack> CREATOR = new Creator<SuggestedTrack>() {
        public SuggestedTrack createFromParcel(Parcel source) {
            return new SuggestedTrack(source);
        }

        public SuggestedTrack[] newArray(int size) {
            return new SuggestedTrack[size];
        }
    };
}