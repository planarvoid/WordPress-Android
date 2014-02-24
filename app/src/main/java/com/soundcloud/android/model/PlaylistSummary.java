package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class PlaylistSummary extends ScModel {

    private String mTitle;
    private UserSummary mUser;
    private List<String> mTags;
    private int mTrackCount;
    private String mArtworkUrl;
    private Date mCreatedAt;

    public PlaylistSummary() {
    }

    public PlaylistSummary(String urn) {
        super(urn);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public UserSummary getUser() {
        return mUser;
    }

    public String getUsername() {
        return mUser.getUsername();
    }

    public void setUser(UserSummary user) {
        this.mUser = user;
    }

    public List<String> getTags() {
        return mTags;
    }

    @JsonProperty("user_tags")
    public void setTags(List<String> tags) {
        this.mTags = tags;
    }

    public int getTrackCount() {
        return mTrackCount;
    }

    @JsonProperty("track_count")
    public void setTrackCount(int trackCount) {
        this.mTrackCount = trackCount;
    }

    public String getArtworkUrl() {
        return mArtworkUrl;
    }

    @JsonProperty("artwork_url")
    public void setArtworkUrl(String artworkUrl) {
        this.mArtworkUrl = artworkUrl;
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.mCreatedAt = createdAt;
    }

    @JsonProperty("_embedded")
    public void setRelatedResources(RelatedResources relatedResources) {
        this.mUser = relatedResources.user;
    }

    private static class RelatedResources {
        private UserSummary user;

        void setUser(UserSummary user) {
            this.user = user;
        }
    }
}
