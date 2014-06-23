package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;

import java.util.Date;
import java.util.List;

public class PlaylistSummary extends ScModel {

    private String title;
    private UserSummary user;
    private List<String> tags;
    private int trackCount;
    private String artworkUrl;
    private Date createdAt;
    private PlayableStats stats;
    private int duration;
    private Sharing sharing;

    /**
     * While we're still using the model hierarchy from public API, we need
     * to convert to these classes on a number of occasions
     */
    public static final Function<PlaylistSummary, Playlist> TO_PLAYLIST = new Function<PlaylistSummary, Playlist>() {
        @Override
        public Playlist apply(PlaylistSummary input) {
            return new Playlist(input);
        }
    };

    /**
     * Required for Jackson
     */
    public PlaylistSummary() {
    }

    public PlaylistSummary(String urn) {
        super(urn);
    }

    @Override
    public PlaylistUrn getUrn() {
        return (PlaylistUrn) super.getUrn();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UserSummary getUser() {
        return user;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public List<String> getTags() {
        return tags;
    }

    @JsonProperty("user_tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getTrackCount() {
        return trackCount;
    }

    @JsonProperty("track_count")
    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    public PlayableStats getStats() {
        return stats;
    }

    public void setStats(PlayableStats stats) {
        this.stats = stats;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isPublic() {
        return sharing.isPublic();
    }

    public Sharing getSharing() {
        return sharing;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    @JsonProperty("artwork_url")
    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
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
