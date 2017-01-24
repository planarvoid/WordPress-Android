package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistUtils.getDuration;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import java.util.List;

@AutoValue
abstract class PlaylistDetailsMetadata extends PlaylistDetailItem implements UpdatablePlaylistItem, ImageResource {

    static PlaylistDetailsMetadata from(Playlist playlist,
                                        List<TrackItem> trackItems,
                                        boolean isLiked,
                                        boolean isEditMode,
                                        int trackCount,
                                        OfflineOptions offlineOptions,
                                        Resources resources,
                                        boolean isOwner) {
        return builder()
                .urn(playlist.urn())
                .title(playlist.title())
                .permalinkUrl(playlist.permalinkUrl())
                .creatorUrn(playlist.creatorUrn())
                .creatorName(playlist.creatorName())
                .canBePlayed(!trackItems.isEmpty())
                .canShuffle(trackItems.size() > 1)
                .trackCount(trackCount)
                .isPrivate(playlist.isPrivate())
                .isRepostedByUser(playlist.isRepostedByCurrentUser().or(false))
                .isLikedByUser(isLiked)
                .likesCount(playlist.likesCount())
                .isMarkedForOffline(playlist.isMarkedForOffline().or(false))
                .showOwnerOptions(isOwner)
                .headerText(PlaylistUtils.getPlaylistInfoLabel(resources, trackCount, getDuration(playlist, trackItems)))
                .offlineOptions(offlineOptions)
                .offlineState(playlist.offlineState().or(OfflineState.NOT_OFFLINE))
                .label(PlaylistUtils.formatPlaylistTitle(resources, playlist.setType(), playlist.isAlbum(), playlist.releaseDate()))
                .imageUrlTemplate(playlist.imageUrlTemplate())
                .isInEditMode(isEditMode)
                .build();
    }

    enum OfflineOptions {
        AVAILABLE, UPSELL, NONE
    }

    PlaylistDetailsMetadata() {
        super(PlaylistDetailItem.Kind.HeaderItem);
    }

    @Override
    public Urn getUrn() {
        return urn();
    }

    abstract public Urn urn();

    abstract public Urn creatorUrn();

    abstract public String creatorName();

    abstract public boolean canBePlayed();

    abstract public boolean canShuffle();

    abstract public int trackCount();

    abstract public int likesCount();

    abstract public boolean isLikedByUser();

    abstract public boolean isPrivate();

    abstract public boolean isRepostedByUser();

    abstract public boolean isMarkedForOffline();

    abstract public boolean showOwnerOptions();

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

    PlaylistDetailsMetadata updatedWithLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = toBuilder().isLikedByUser(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    PlaylistDetailsMetadata updatedWithRepostStatus(RepostsStatusEvent.RepostStatus repostStatus) {
        return toBuilder().isRepostedByUser(repostStatus.isReposted()).build();
    }

    static Builder builder() {
        return new AutoValue_PlaylistDetailsMetadata.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder urn(Urn value);

        abstract Builder creatorUrn(Urn value);

        abstract Builder creatorName(String value);

        abstract Builder canBePlayed(boolean value);

        abstract Builder canShuffle(boolean value);

        abstract Builder trackCount(int value);

        abstract Builder likesCount(int value);

        abstract Builder isLikedByUser(boolean value);

        abstract Builder isPrivate(boolean value);

        abstract Builder isRepostedByUser(boolean value);

        abstract Builder isMarkedForOffline(boolean value);

        abstract Builder showOwnerOptions(boolean value);

        abstract Builder offlineState(OfflineState value);

        abstract Builder offlineOptions(OfflineOptions value);

        abstract Builder permalinkUrl(Optional<String> value);

        abstract Builder title(String value);

        abstract Builder label(String value);

        abstract Builder headerText(String value);

        abstract Builder imageUrlTemplate(Optional<String> value);

        abstract Builder isInEditMode(boolean value);

        abstract PlaylistDetailsMetadata build();
    }


}
