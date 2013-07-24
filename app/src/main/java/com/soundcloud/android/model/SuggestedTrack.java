package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}