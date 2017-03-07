package com.soundcloud.android.tracks;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

@AutoValue
public abstract class Track {

    public abstract Urn urn();

    public abstract String title();

    public abstract long snippetDuration();

    public abstract long fullDuration();

    public abstract int playCount();

    public abstract int commentsCount();

    public abstract int likesCount();

    public abstract int repostsCount();

    public abstract boolean commentable();

    public abstract boolean monetizable();

    public abstract boolean blocked();

    public abstract boolean snipped();

    public abstract boolean isPrivate();

    public abstract boolean subHighTier();

    public abstract boolean subMidTier();

    public abstract String monetizationModel();

    public abstract String permalinkUrl();

    public abstract Date createdAt();

    public abstract boolean userLike();

    public abstract boolean userRepost();

    public abstract Optional<String> imageUrlTemplate();

    public abstract String policy();

    public abstract String waveformUrl();

    public abstract String creatorName();

    public abstract Urn creatorUrn();

    public abstract OfflineState offlineState();

    public abstract Optional<String> description();

    public abstract Optional<String> genre();

    @VisibleForTesting
    public static Builder builder(Track track) {
        return new AutoValue_Track.Builder(track);
    }

    public static Builder builder() {
        return new AutoValue_Track.Builder();
    }

    public static Track copyWithDescription(Track track, Optional<String> description) {
        return new AutoValue_Track.Builder(track).description(description).build();
    }

    public static Track from(ApiTrack apiTrack) {
        return from(apiTrack, false, false);
    }

    public static Track from(ApiTrack apiTrack, boolean isRepost, boolean isUserLike) {
        final Optional<Boolean> subHighTier = apiTrack.isSubHighTier();
        final Optional<Boolean> subMidTier = apiTrack.isSubMidTier();
        final Builder builder = new AutoValue_Track.Builder()
                .urn(apiTrack.getUrn())
                .title(apiTrack.getTitle())
                .createdAt(apiTrack.getCreatedAt())
                .snippetDuration(apiTrack.getSnippetDuration())
                .fullDuration(apiTrack.getFullDuration())
                .isPrivate(apiTrack.isPrivate())
                .waveformUrl(Optional.fromNullable(apiTrack.getWaveformUrl()).or(Strings.EMPTY))
                .permalinkUrl(apiTrack.getPermalinkUrl())
                .monetizable(apiTrack.isMonetizable())
                .blocked(apiTrack.isBlocked())
                .snipped(apiTrack.isSnipped())
                .policy(apiTrack.getPolicy())
                .subHighTier(subHighTier.isPresent() ? subHighTier.get() : false)
                .subMidTier(subMidTier.isPresent() ? subMidTier.get() : false)
                .playCount(apiTrack.getStats().getPlaybackCount())
                .commentsCount(apiTrack.getStats().getCommentsCount())
                .likesCount(apiTrack.getStats().getLikesCount())
                .repostsCount(apiTrack.getStats().getRepostsCount())
                .creatorName(apiTrack.getUser() != null ? apiTrack.getUser().getUsername() : Strings.EMPTY)
                .creatorUrn(apiTrack.getUser() != null ? apiTrack.getUser().getUrn() : Urn.NOT_SET)
                .imageUrlTemplate(apiTrack.getImageUrlTemplate())
                .genre(Optional.fromNullable(apiTrack.getGenre()))
                .monetizationModel(apiTrack.getMonetizationModel().or(Strings.EMPTY))
                .commentable(apiTrack.isCommentable())
                .userLike(isUserLike)
                .userRepost(isRepost)
                .offlineState(OfflineState.NOT_OFFLINE)
                .description(Optional.absent());


        return builder.build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder urn(Urn urn);

        public abstract Builder title(String title);

        public abstract Builder snippetDuration(long snippetDuration);

        public abstract Builder fullDuration(long fullDuration);

        public abstract Builder playCount(int playCount);

        public abstract Builder commentsCount(int commentsCount);

        public abstract Builder likesCount(int likesCount);

        public abstract Builder repostsCount(int repostsCount);

        public abstract Builder commentable(boolean commentable);

        public abstract Builder monetizable(boolean monetizable);

        public abstract Builder blocked(boolean blocked);

        public abstract Builder snipped(boolean snipped);

        public abstract Builder isPrivate(boolean isPrivate);

        public abstract Builder subHighTier(boolean subHighTier);

        public abstract Builder subMidTier(boolean subMidTier);

        public abstract Builder monetizationModel(String monetizationModel);

        public abstract Builder permalinkUrl(String permalinkUrl);

        public abstract Builder createdAt(Date createdAt);

        public abstract Builder userLike(boolean userLike);

        public abstract Builder userRepost(boolean userRepost);

        public abstract Builder imageUrlTemplate(Optional<String> imageUrlTemplate);

        public abstract Builder policy(String policy);

        public abstract Builder waveformUrl(String waveformUrl);

        public abstract Builder creatorName(String creatorName);

        public abstract Builder creatorUrn(Urn creatorUrn);

        public abstract Builder offlineState(OfflineState offlineState);

        public abstract Builder description(Optional<String> creatorUrn);

        public abstract Builder genre(Optional<String> creatorUrn);

        public abstract Track build();
    }
}
