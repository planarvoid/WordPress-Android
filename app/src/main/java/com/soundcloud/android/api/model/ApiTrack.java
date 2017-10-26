package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class ApiTrack implements ApiEntityHolder, TrackRecord, TrackRecordHolder, ApiSyncable {

    @Override
    public abstract Urn getUrn();

    public long getId() {
        return getUrn().getNumericId();
    }

    public abstract String getTitle();

    @Nullable
    public abstract String getGenre();

    public abstract UserRecord getUser();

    public String getUserName() {
        UserRecord user = getUser();
        return user != null ? user.getUsername() : Strings.EMPTY;
    }

    public abstract boolean isCommentable();

    public abstract long getSnippetDuration();

    public abstract long getFullDuration();

    @Nullable
    public abstract String getWaveformUrl();

    public abstract Optional<String> getImageUrlTemplate();

    public abstract String getPermalinkUrl();

    public abstract ApiTrackStats getStats();

    public abstract List<String> getUserTags();

    public abstract Date getCreatedAt();

    public abstract Sharing getSharing();

    public abstract boolean isMonetizable();

    public abstract boolean isBlocked();

    public abstract boolean isSnipped();

    public abstract String getPolicy();

    public abstract Optional<String> getMonetizationModel();

    public abstract Optional<Boolean> getIsSubMidTier();

    public abstract Optional<Boolean> getIsSubHighTier();

    public abstract Optional<String> getSecretToken();

    public abstract boolean isSyncable();

    @Override
    public int getPlaybackCount() {
        return getStats().getPlaybackCount();
    }

    @Override
    public int getCommentsCount() {
        return getStats().getCommentsCount();
    }

    @Override
    public int getLikesCount() {
        return getStats().getLikesCount();
    }

    @Override
    public int getRepostsCount() {
        return getStats().getRepostsCount();
    }

    @Override
    public abstract Optional<String> getDescription();

    @Override
    public abstract boolean isDisplayStatsEnabled();

    public Boolean isPrivate() {
        return getSharing() != Sharing.PUBLIC;
    }

    @Override
    public TrackRecord getTrackRecord() {
        return this;
    }

    @JsonCreator
    public static ApiTrack create(@JsonProperty("urn") Urn urn,
                                  @JsonProperty("title") String title,
                                  @JsonProperty("genre") String genre,
                                  @JsonProperty("_embedded") RelatedResources relatedResources,
                                  @JsonProperty("commentable") boolean commentable,
                                  @JsonProperty("snip_duration") long snippetDuration,
                                  @JsonProperty("full_duration") long fullDuration,
                                  @JsonProperty("waveform_url") String waveformUrl,
                                  @JsonProperty("artwork_url_template") String artworkUrlTemplate,
                                  @JsonProperty("permalink_url") String permalinkUrl,
                                  @JsonProperty("user_tags") List<String> userTags,
                                  @JsonProperty("created_at") Date createdAt,
                                  @JsonProperty("sharing") Sharing sharing,
                                  @JsonProperty("monetizable") boolean monetizable,
                                  @JsonProperty("blocked") boolean blocked,
                                  @JsonProperty("snipped") boolean snipped,
                                  @JsonProperty("policy") String policy,
                                  @JsonProperty("monetization_model") String monetizationModel,
                                  @JsonProperty("sub_mid_tier") boolean subMidTier,
                                  @JsonProperty("sub_high_tier") boolean subHighTier,
                                  @JsonProperty("secret_token") String secretToken,
                                  @JsonProperty("syncable") boolean syncable,
                                  @JsonProperty("description") String description,
                                  @JsonProperty("display_stats") boolean displayStatsEnabled) {
        return builder(urn)
                .title(title)
                .genre(genre)
                .user(relatedResources.user)
                .commentable(commentable)
                .snippetDuration(snippetDuration)
                .fullDuration(fullDuration)
                .waveformUrl(waveformUrl)
                .imageUrlTemplate(Optional.fromNullable(artworkUrlTemplate))
                .permalinkUrl(permalinkUrl)
                .stats(relatedResources.stats)
                .userTags(userTags)
                .createdAt(createdAt)
                .sharing(sharing)
                .monetizable(monetizable)
                .blocked(blocked)
                .snipped(snipped)
                .policy(policy)
                .monetizationModel(Optional.fromNullable(monetizationModel))
                .isSubMidTier(Optional.fromNullable(subMidTier))
                .isSubHighTier(Optional.fromNullable(subHighTier))
                .secretToken(Optional.fromNullable(secretToken))
                .syncable(syncable)
                .description(Optional.fromNullable(description))
                .displayStatsEnabled(displayStatsEnabled)
                .build();
    }

    public abstract ApiTrack.Builder toBuilder();

    public static Builder builder(Urn urn) {
        return new AutoValue_ApiTrack.Builder()
                .urn(urn)
                .secretToken(Optional.absent());
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

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder urn(Urn newUrn);

        public abstract Builder title(String newTitle);

        public abstract Builder genre(String newGenre);

        public abstract Builder user(UserRecord newUser);

        public abstract Builder commentable(boolean newCommentable);

        public abstract Builder snippetDuration(long newSnippetDuration);

        public abstract Builder fullDuration(long newFullDuration);

        public abstract Builder waveformUrl(String newWaveformUrl);

        public abstract Builder imageUrlTemplate(Optional<String> newImageUrlTemplate);

        public abstract Builder permalinkUrl(String newPermalinkUrl);

        public abstract Builder stats(ApiTrackStats newStats);

        public abstract Builder userTags(List<String> newUserTags);

        public abstract Builder createdAt(Date newCreatedAt);

        public abstract Builder sharing(Sharing newSharing);

        public abstract Builder monetizable(boolean newMonetizable);

        public abstract Builder blocked(boolean newBlocked);

        public abstract Builder snipped(boolean newSnipped);

        public abstract Builder policy(String newPolicy);

        public abstract Builder monetizationModel(Optional<String> newMonetizationModel);

        public abstract Builder isSubMidTier(Optional<Boolean> newSubMidTier);

        public abstract Builder isSubHighTier(Optional<Boolean> newSubHighTier);

        public abstract Builder secretToken(Optional<String> secretToken);

        public abstract Builder syncable(boolean newSyncable);

        public abstract Builder description(Optional<String> newDescription);

        public abstract Builder displayStatsEnabled(boolean newDisplayStatsEnabled);

        public abstract ApiTrack build();
    }
}
