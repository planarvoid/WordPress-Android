package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.legacy.model.PlayableStats;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

public class ApiPlaylist implements ImageResource, ApiEntityHolder, PlaylistRecord, PlaylistRecordHolder, ApiSyncable {

    private Urn urn;
    private String title;
    private ApiUser user;
    private String genre;
    private List<String> tags;
    private int trackCount;
    private Optional<String> artworkUrlTemplate = Optional.absent();
    private Date createdAt;
    private PlayableStats stats;
    private long duration;
    private Sharing sharing;
    private String permalinkUrl;
    private boolean isAlbum;
    private String setType;
    private String releaseDate;

    /**
     * Required for Jackson
     */
    public ApiPlaylist() {
    }

    ApiPlaylist(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    public void setUrn(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return artworkUrlTemplate;
    }

    @JsonProperty("artwork_url_template")
    public void setArtworkUrlTemplate(String artworkUrlTemplate) {
        this.artworkUrlTemplate = Optional.fromNullable(artworkUrlTemplate);
    }

    public long getId() {
        return urn.getNumericId();
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

    @Override
    public String getGenre() {
        return genre;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setUser(ApiUser user) {
        this.user = user;
    }

    @JsonProperty("genre")
    public void setGenre(String genre) {
        this.genre = genre;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
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

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    @Override
    public int getLikesCount() {
        return stats.getLikesCount();
    }

    @Override
    public int getRepostsCount() {
        return stats.getRepostsCount();
    }

    @JsonProperty("permalink_url")
    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
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

    @JsonProperty("is_album")
    public void setIsAlbum(boolean isAlbum) {
        this.isAlbum = isAlbum;
    }

    public boolean isAlbum() {
        return isAlbum;
    }

    @JsonProperty("set_type")
    public void setSetType(String setType) {
        this.setType = setType;
    }

    public String getSetType() {
        return setType;
    }

    @JsonProperty("release_date")
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiPlaylist that = (ApiPlaylist) o;
        return MoreObjects.equal(urn, that.urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public PlaylistRecord getPlaylistRecord() {
        return this;
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

    @Override
    public EntityStateChangedEvent toUpdateEvent() {
        return PlaylistItem.from(this).toUpdateEvent();
    }
}
