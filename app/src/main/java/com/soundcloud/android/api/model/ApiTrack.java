package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

public final class ApiTrack implements ApiEntityHolder, TrackRecord, TrackRecordHolder, ApiSyncable {

    private Urn urn;
    private String title;
    private String genre;
    private ApiUser user;
    private boolean commentable;
    private long snippetDuration = Consts.NOT_SET;
    private long fullDuration = Consts.NOT_SET;
    private String streamUrl;
    private String waveformUrl;
    private List<String> userTags;
    private Date createdAt;
    private Optional<String> artworkUrlTemplate = Optional.absent();
    private String permalinkUrl;
    private Sharing sharing = Sharing.UNDEFINED;
    private Optional<String> description = Optional.absent();
    private ApiTrackStats stats;

    private boolean monetizable;
    private String policy;
    private boolean syncable;
    private boolean blocked;
    private boolean snipped;

    private Optional<String> monetizationModel = Optional.absent();
    private Optional<Boolean> subMidTier = Optional.absent();
    private Optional<Boolean> subHighTier = Optional.absent();


    public ApiTrack() { /* for Deserialization */ }

    ApiTrack(Urn urn) {
        this.urn = urn;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    public void setUrn(Urn urn) {
        this.urn = urn;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public UserRecord getUser() {
        return user;
    }

    public void setUser(ApiUser user) {
        this.user = user;
    }

    public String getUserName() {
        return user != null ? user.getUsername() : "";
    }

    public boolean isCommentable() {
        return commentable;
    }

    public void setCommentable(boolean commentable) {
        this.commentable = commentable;
    }

    public long getSnippetDuration() {
        return snippetDuration;
    }

    public long getFullDuration() {
        return fullDuration;
    }

    @JsonProperty("snip_duration")
    public void setSnippetDuration(long snippetDuration) {
        this.snippetDuration = snippetDuration;
    }

    @JsonProperty("full_duration")
    public void setFullDuration(long fullDuration) {
        this.fullDuration = fullDuration;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    @JsonProperty("stream_url")
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getWaveformUrl() {
        return waveformUrl;
    }

    @JsonProperty("waveform_url")
    public void setWaveformUrl(String waveformUrl) {
        this.waveformUrl = waveformUrl;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return artworkUrlTemplate;
    }

    @JsonProperty("artwork_url_template")
    public void setArtworkUrlTemplate(String template) {
        this.artworkUrlTemplate = Optional.fromNullable(template);
    }

    public String getPermalinkUrl() {
        return permalinkUrl;
    }

    @JsonProperty("permalink_url")
    public void setPermalinkUrl(String permalinkUrl) {
        this.permalinkUrl = permalinkUrl;
    }

    public ApiTrackStats getStats() {
        return stats;
    }

    public void setStats(ApiTrackStats stats) {
        this.stats = stats;
    }

    public List<String> getUserTags() {
        return userTags;
    }

    @JsonProperty("user_tags")
    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Sharing getSharing() {
        return sharing;
    }

    public void setSharing(Sharing sharing) {
        this.sharing = sharing;
    }

    public boolean isMonetizable() {
        return monetizable;
    }

    @JsonProperty("monetizable")
    public void setMonetizable(boolean monetizable) {
        this.monetizable = monetizable;
    }

    public boolean isBlocked() {
        return blocked;
    }

    @JsonProperty("snipped")
    public void setSnipped(boolean snipped) {
        this.snipped = snipped;
    }

    public boolean isSnipped() {
        return snipped;
    }

    @JsonProperty("blocked")
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getPolicy() {
        return policy;
    }

    @JsonProperty("policy")
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @JsonProperty("monetization_model")
    public void setMonetizationModel(String monetizationModel) {
        this.monetizationModel = Optional.of(monetizationModel);
    }

    public Optional<String> getMonetizationModel() {
        return monetizationModel;
    }

    @JsonProperty("sub_mid_tier")
    public void setSubMidTier(boolean subMidTier) {
        this.subMidTier = Optional.of(subMidTier);
    }

    public Optional<Boolean> isSubMidTier() {
        return subMidTier;
    }

    @JsonProperty("sub_high_tier")
    public void setSubHighTier(boolean subHighTier) {
        this.subHighTier = Optional.of(subHighTier);
    }

    public Optional<Boolean> isSubHighTier() {
        return subHighTier;
    }

    public boolean isSyncable() {
        return syncable;
    }

    @Override
    public int getPlaybackCount() {
        return stats.getPlaybackCount();
    }

    @Override
    public int getCommentsCount() {
        return stats.getCommentsCount();
    }

    @Override
    public int getLikesCount() {
        return stats.getLikesCount();
    }

    @Override
    public int getRepostsCount() {
        return stats.getRepostsCount();
    }

    @Override
    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Optional.fromNullable(description);
    }

    @JsonProperty("syncable")
    public void setSyncable(boolean syncable) {
        this.syncable = syncable;
    }

    @JsonProperty("_embedded")
    public void setRelatedResources(RelatedResources relatedResources) {
        this.user = relatedResources.user;
        this.stats = relatedResources.stats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiTrack apiTrack = (ApiTrack) o;
        return MoreObjects.equal(urn, apiTrack.urn);
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    public Boolean isPrivate() {
        return getSharing() != Sharing.PUBLIC;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("title", title)
                          .add("genre", genre)
                          .add("user", user)
                          .add("commentable", commentable)
                          .add("snippetDuration", snippetDuration)
                          .add("fullDuration", fullDuration)
                          .add("streamUrl", streamUrl)
                          .add("waveformUrl", waveformUrl)
                          .add("userTags", userTags)
                          .add("createdAt", createdAt)
                          .add("artworkUrl", artworkUrlTemplate)
                          .add("permalinkUrl", permalinkUrl)
                          .add("monetizable", monetizable)
                          .add("snipped", snipped)
                          .add("blocked", blocked)
                          .add("syncable", syncable)
                          .add("policy", policy)
                          .add("sharing", sharing)
                          .add("stats", stats).toString();
    }

    @Override
    public TrackRecord getTrackRecord() {
        return this;
    }

    private static class RelatedResources {
        private ApiUser user;
        private ApiTrackStats stats;

        void setUser(ApiUser user) {
            this.user = user;
        }

        void setStats(ApiTrackStats stats) {
            this.stats = stats;
        }
    }
}
