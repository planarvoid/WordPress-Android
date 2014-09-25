package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.legacy.model.PlayableStats;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;
import java.util.List;

public class ApiPlaylist extends ScModel implements PropertySetSource {

    private String title;
    private ApiUser user;
    private List<String> tags;
    private int trackCount;
    private String artworkUrl;
    private Date createdAt;
    private PlayableStats stats;
    private int duration;
    private Sharing sharing;
    private String permalinkUrn;

    /**
     * Required for Jackson
     */
    public ApiPlaylist() {
    }

    public ApiPlaylist(String urn) {
        super(urn);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ApiUser getUser() {
        return user;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setUser(ApiUser user) {
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

    public String getPermalinkUrn() {
        return permalinkUrn;
    }

    @JsonProperty("permalink_urn")
    public void setPermalinkUrn(String permalinkUrn) {
        this.permalinkUrn = permalinkUrn;
    }

    @Deprecated
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

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                PlaylistProperty.URN.bind(getUrn()),
                PlaylistProperty.TITLE.bind(getTitle()),
                PlaylistProperty.CREATED_AT.bind(getCreatedAt()),
                PlaylistProperty.DURATION.bind(getDuration()),
                PlaylistProperty.PERMALINK_URL.bind(getPermalinkUrn()),
                PlaylistProperty.IS_PRIVATE.bind(!isPublic()),
                PlaylistProperty.TRACK_COUNT.bind(getTrackCount()),
                PlaylistProperty.LIKES_COUNT.bind(getStats().getLikesCount()),
                PlaylistProperty.REPOSTS_COUNT.bind(getStats().getRepostsCount()),
                PlaylistProperty.CREATOR_NAME.bind(getUsername()),
                PlaylistProperty.CREATOR_URN.bind(getUser() != null ? getUser().getUrn() : Urn.NOT_SET)
        );
    }

    private static class RelatedResources {
        private ApiUser user;
        private PlayableStats stats;

        void setUser(ApiUser user) {
            this.user = user;
        }

        void setStats(PlayableStats stats) {
            this.stats = stats;
        }
    }
}
