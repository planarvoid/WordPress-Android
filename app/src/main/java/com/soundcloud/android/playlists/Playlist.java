package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class Playlist {

    public static Playlist from(ApiPlaylist playlist) {
        return builder()
                .urn(playlist.getUrn())
                .title(playlist.getTitle())
                .creatorUrn(playlist.getUser().getUrn())
                .creatorName(playlist.getUser().getUsername())
                .genre(playlist.getGenre())
                .duration(playlist.getDuration())
                .trackCount(playlist.getTrackCount())
                .isPrivate(!playlist.isPublic())
                .imageUrlTemplate(playlist.getImageUrlTemplate())
                .likesCount(playlist.getLikesCount())
                .repostCount(playlist.getRepostsCount())
                .setType(playlist.getSetType())
                .permalinkUrl(Optional.of(playlist.getPermalinkUrl()))
                .isAlbum(playlist.isAlbum())
                .releaseDate(playlist.getReleaseDate())
                .tags(playlist.getTags())
                .createdAt(playlist.getCreatedAt())
                .build();
    }

    public abstract Urn urn();

    public abstract String title();

    public abstract Urn creatorUrn();

    public abstract String creatorName();

    public abstract long duration();

    public abstract int trackCount();

    public abstract boolean isPrivate();

    public abstract Optional<String> genre();

    public abstract int likesCount();

    public abstract int repostCount();

    public abstract Optional<String> permalinkUrl(); // local playlists do not have permalinks (not synced yet)

    public abstract Optional<String> setType();

    public abstract Date createdAt();

    public abstract boolean isAlbum();

    public abstract Optional<String> releaseDate();

    public abstract Optional<List<String>> tags();

    public abstract Optional<Boolean> isLikedByCurrentUser();

    public abstract Optional<Boolean> isMarkedForOffline();

    public abstract Optional<OfflineState> offlineState();

    public abstract Optional<Boolean> isRepostedByCurrentUser();

    public abstract Optional<String> imageUrlTemplate();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_Playlist.Builder()
                .permalinkUrl(Optional.absent())
                .tags(Optional.absent())
                .isMarkedForOffline(Optional.absent())
                .isLikedByCurrentUser(Optional.absent())
                .isRepostedByCurrentUser(Optional.absent())
                .offlineState(Optional.absent())
                .imageUrlTemplate(Optional.absent());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder urn(Urn value);

        public abstract Builder title(String value);

        public abstract Builder creatorUrn(Urn value);

        public abstract Builder creatorName(String value);

        public abstract Builder duration(long value);

        public abstract Builder trackCount(int value);

        public abstract Builder isPrivate(boolean value);

        public Builder genre(String genre) {
            return genre(Optional.fromNullable(genre));
        }

        public abstract Builder genre(Optional<String> genre);

        public Builder isLikedByCurrentUser(boolean value) {
            return isLikedByCurrentUser(Optional.of(value));
        }

        public abstract Builder likesCount(int value);

        public abstract Builder repostCount(int value);

        public Builder isMarkedForOffline(boolean isMarkedForOffline) {
            return isMarkedForOffline(Optional.of(isMarkedForOffline));
        }

        public abstract Builder permalinkUrl(Optional<String> value);

        public Builder permalinkUrl(String value) {
            return permalinkUrl(Optional.of(value));
        }

        public Builder setType(String setType) {
            return setType(Optional.fromNullable(setType));
        }

        public abstract Builder setType(Optional<String> setType);

        public abstract Builder createdAt(Date date);

        public Builder offlineState(OfflineState offlineState) {
            return offlineState(Optional.of(offlineState));
        }

        public abstract Builder isAlbum(boolean value);

        public Builder releaseDate(String releaseDate) {
            return releaseDate(Optional.fromNullable(releaseDate));
        }

        public abstract Builder releaseDate(Optional<String> releaseDate);

        public Builder tags(List<String> value) {
            return tags(Optional.of(value));
        }

        public abstract Builder tags(Optional<List<String>> value);

        public abstract Builder isLikedByCurrentUser(Optional<Boolean> value);

        public Builder isRepostedByCurrentUser(boolean value) {
            return isRepostedByCurrentUser(Optional.fromNullable(value));
        }

        public abstract Builder isMarkedForOffline(Optional<Boolean> value);

        public abstract Builder offlineState(Optional<OfflineState> value);

        public abstract Builder isRepostedByCurrentUser(Optional<Boolean> value);

        public abstract Builder imageUrlTemplate(Optional<String> value);

        public abstract Playlist build();
    }
}
