package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;

import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoValue
abstract class PlaylistDetailHeaderItem extends PlaylistDetailItem implements UpdatablePlaylistItem, ImageResource {

    PlaylistDetailHeaderItem() {
        super(PlaylistDetailItem.Kind.HeaderItem);
    }

    public static PlaylistDetailHeaderItem from(Playlist playlist, List<TrackItem> tracks, boolean isLiked, Resources resources) {
        return builder()
                .urn(playlist.urn())
                .title(playlist.title())
                .permalinkUrl(playlist.permalinkUrl())
                .creatorUrn(playlist.creatorUrn())
                .creatorName(playlist.creatorName())
                .canBePlayed(!tracks.isEmpty())
                .trackCount(tracks.isEmpty() ? playlist.trackCount() : tracks.size())
                .duration(getDuration(playlist, tracks))
                .isPrivate(playlist.isPrivate())
                .isRepostedByUser(playlist.isRepostedByCurrentUser().or(false))
                .isLikedByUser(isLiked)
                .likesCount(playlist.likesCount())
                .isMarkedForOffline(playlist.isMarkedForOffline().or(false))
                .offlineState(playlist.offlineState().or(OfflineState.NOT_OFFLINE))
                .label(PlaylistUtils.formatPlaylistTitle(resources, playlist.setType(), playlist.isAlbum(), playlist.releaseDate()))
                .imageUrlTemplate(playlist.imageUrlTemplate())
                .build();
    }

    public static String getDuration(Playlist playlist, List<TrackItem> tracks) {
        final long duration = tracks.isEmpty() ?
                              playlist.duration() :
                              getCombinedTrackDurations(tracks);
        return ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS);
    }

    private static long getCombinedTrackDurations(List<TrackItem> tracks) {
        long duration = 0;
        for (TrackItem track : tracks) {
            duration += track.getDuration();
        }
        return duration;
    }

    @Override
    public Urn getUrn() {
        return urn();
    }

    abstract Urn urn();

    abstract public Urn creatorUrn();

    abstract public String creatorName();

    abstract public boolean canBePlayed();

    abstract public int trackCount();

    abstract String duration();

    abstract public int likesCount();

    abstract public boolean isLikedByUser();

    abstract public boolean isPrivate();

    abstract public boolean isRepostedByUser();

    abstract public boolean isMarkedForOffline();

    abstract public OfflineState offlineState();

    abstract public Optional<String> permalinkUrl();

    abstract public String title();

    abstract String label();

    abstract Optional<String> imageUrlTemplate();

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

    PlaylistDetailHeaderItem updatedWithLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
        final Builder builder = toBuilder().isLikedByUser(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return builder.build();
    }

    PlaylistDetailHeaderItem updatedWithRepostStatus(RepostsStatusEvent.RepostStatus repostStatus) {
        return toBuilder().isRepostedByUser(repostStatus.isReposted()).build();
    }

    static Builder builder() {
        return new AutoValue_PlaylistDetailHeaderItem.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder urn(Urn value);

        abstract Builder creatorUrn(Urn value);

        abstract Builder canBePlayed(boolean value);

        abstract Builder trackCount(int value);

        abstract Builder duration(String value);

        abstract Builder likesCount(int value);

        abstract Builder isLikedByUser(boolean value);

        abstract Builder isPrivate(boolean value);

        abstract Builder isRepostedByUser(boolean value);

        abstract Builder isMarkedForOffline(boolean value);

        abstract Builder offlineState(OfflineState value);

        abstract Builder permalinkUrl(Optional<String> value);

        abstract Builder title(String value);

        abstract Builder creatorName(String value);

        abstract Builder label(String value);

        abstract Builder imageUrlTemplate(Optional<String> value);

        abstract PlaylistDetailHeaderItem build();
    }
}
