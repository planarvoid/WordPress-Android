package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistUtils.getDuration;
import static java.util.Collections.emptyList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

import java.util.List;

@AutoValue
abstract class PlaylistDetailsMetadata implements UpdatablePlaylistItem, ImageResource {

    enum OfflineOptions {
        AVAILABLE, UPSELL, NONE
    }

    @Override
    public Urn getUrn() {
        return urn();
    }

    abstract public Urn urn();

    abstract public Urn creatorUrn();

    abstract public String creatorName();

    abstract boolean canBePlayed();

    abstract public boolean canShuffle();

    abstract public int trackCount();

    abstract public int likesCount();

    abstract public boolean isLikedByUser();

    abstract public boolean isPrivate();

    abstract public boolean isRepostedByUser();

    abstract public boolean isMarkedForOffline();

    abstract public boolean isOwner();

    abstract public OfflineState offlineState();

    abstract public OfflineOptions offlineOptions();

    abstract public Optional<String> permalinkUrl();

    abstract public String title();

    abstract public String label();

    abstract public String headerText();

    abstract public Optional<String> imageUrlTemplate();

    abstract public boolean isInEditMode();

    public abstract Builder toBuilder();

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrlTemplate();
    }

    @Override
    public UpdatablePlaylistItem updatedWithTrackCount(int trackCount) {
        return toBuilder().trackCount(trackCount).build();
    }

    @Override
    public UpdatablePlaylistItem updatedWithMarkedForOffline(boolean isMarkedForOffline) {
        return toBuilder().isMarkedForOffline(isMarkedForOffline).build();
    }

    @Override
    public UpdatablePlaylistItem updatedWithPlaylist(Playlist playlist) {
        return toBuilder()
                .urn(playlist.urn())
                .title(playlist.title())
                .permalinkUrl(playlist.permalinkUrl())
                .creatorUrn(playlist.creatorUrn())
                .creatorName(playlist.creatorName())
                .isPrivate(playlist.isPrivate())
                .isRepostedByUser(playlist.isRepostedByCurrentUser().or(false))
                .likesCount(playlist.likesCount())
                .isMarkedForOffline(playlist.isMarkedForOffline().or(false))
                .offlineState(playlist.offlineState().or(OfflineState.NOT_OFFLINE))
                .imageUrlTemplate(playlist.imageUrlTemplate())
                .build();
    }

    static Builder builder() {
        return new AutoValue_PlaylistDetailsMetadata.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        static PlaylistDetailsMetadata.OfflineOptions toOfflineOptions(FeatureOperations featureOperations) {
            if (featureOperations.isOfflineContentEnabled()) {
                return PlaylistDetailsMetadata.OfflineOptions.AVAILABLE;
            } else if (featureOperations.upsellOfflineContent()) {
                return PlaylistDetailsMetadata.OfflineOptions.UPSELL;
            } else {
                return PlaylistDetailsMetadata.OfflineOptions.NONE;
            }
        }

        protected abstract Builder urn(Urn value);

        protected abstract Builder creatorUrn(Urn value);

        protected abstract Builder creatorName(String value);

        protected abstract Builder canShuffle(boolean value);

        protected abstract Builder canBePlayed(boolean value);

        protected abstract Builder trackCount(int value);

        protected abstract Builder likesCount(int value);

        protected abstract Builder isPrivate(boolean value);

        protected abstract Builder isMarkedForOffline(boolean value);

        protected abstract Builder isOwner(boolean value);

        protected abstract Builder offlineState(OfflineState value);

        protected abstract Builder offlineOptions(OfflineOptions value);

        protected abstract Builder permalinkUrl(Optional<String> value);

        protected abstract Builder title(String value);

        protected abstract Builder label(String value);

        protected abstract Builder headerText(String value);

        protected abstract Builder imageUrlTemplate(Optional<String> value);

        abstract Builder isInEditMode(boolean value);

        abstract Builder isRepostedByUser(boolean value);

        abstract Builder isLikedByUser(boolean value);

        abstract PlaylistDetailsMetadata build();

        public Builder with(Resources resources, FeatureOperations featureOperations, AccountOperations accountOperations, Playlist playlist) {
            return with(resources, featureOperations, accountOperations, playlist, emptyList());
        }

        public Builder with(Resources resources,
                            FeatureOperations featureOperations,
                            AccountOperations accountOperations,
                            Playlist playlist,
                            List<TrackItem> trackItems) {
            return with(resources, playlist, trackItems, accountOperations.isLoggedInUser(playlist.creatorUrn()), toOfflineOptions(featureOperations));
        }

        public Builder with(Resources resources,
                            Playlist playlist,
                            List<TrackItem> trackItems,
                            boolean isOwner,
                            OfflineOptions offlineOptions) {
            final int trackCount = trackItems.isEmpty() ? playlist.trackCount() : trackItems.size();
            final String headerText = PlaylistUtils.getPlaylistInfoLabel(resources, trackCount, getDuration(playlist, trackItems));
            final String label = PlaylistUtils.formatPlaylistTitle(resources, playlist.setType().or(Strings.EMPTY), playlist.isAlbum(), playlist.releaseDate().or(Strings.EMPTY));

            return urn(playlist.urn())
                    .title(playlist.title())
                    .permalinkUrl(playlist.permalinkUrl())
                    .creatorUrn(playlist.creatorUrn())
                    .creatorName(playlist.creatorName())
                    .isPrivate(playlist.isPrivate())
                    .likesCount(playlist.likesCount())
                    .imageUrlTemplate(playlist.imageUrlTemplate())
                    .label(label)
                    .canShuffle(trackItems.size() > 1)
                    .canBePlayed(!trackItems.isEmpty())
                    .headerText(headerText)
                    .trackCount(trackCount)
                    .isOwner(isOwner)
                    .offlineOptions(offlineOptions);
        }

        public Builder with(Resources resources, List<TrackItem> trackItems) {
            final int trackCount = trackItems.size();
            final String headerText = PlaylistUtils.getPlaylistInfoLabel(resources, trackCount, getDuration(trackItems));

            return headerText(headerText)
                    .canShuffle(trackCount > 1)
                    .canBePlayed(trackCount > 0)
                    .trackCount(trackCount);
        }

        public Builder with(OfflineState offlineState) {
            return isMarkedForOffline(offlineState != OfflineState.NOT_OFFLINE)
                    .offlineState(offlineState);
        }
    }
}
