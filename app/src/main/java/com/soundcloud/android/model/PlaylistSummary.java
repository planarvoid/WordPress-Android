package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.utils.images.ImageUtils;

import java.util.Date;
import java.util.List;

public class PlaylistSummary extends ScModel {

    private String mTitle;
    private UserSummary mUser;
    private List<String> mTags;
    private int mTrackCount;
    private String mArtworkUrl;
    private Date mCreatedAt;
    private PlayableStats mStats;

    /**
     * Required for Jackson
     */
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

    public PlayableStats getStats() {
        return mStats;
    }

    public void setStats(PlayableStats stats) {
        mStats = stats;
    }

    public String getArtworkUrl() {
        return mArtworkUrl;
    }

    public String getArtworkUrl(ImageSize imageSize) {
        if (ImageUtils.checkIconShouldLoad(mArtworkUrl)) {
            return imageSize.formatUri(mArtworkUrl);
        }
        return null;
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
        this.mStats = relatedResources.stats;
    }

    private static class RelatedResources {
        private UserSummary user;
        private PlayableStats stats;

        void setUser(UserSummary user) {
            this.user = user;
        }

        void setStats(PlayableStats stats) {
            this.stats = stats;
        }
    }
}
