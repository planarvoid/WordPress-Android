package com.soundcloud.android.playlists;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistWithTracks implements ImageResource, UpdatablePlaylistItem {

    @NotNull private final Playlist playlist;
    @NotNull private final List<TrackItem> tracks;

    public PlaylistWithTracks(@NotNull Playlist playlist, @NotNull List<TrackItem> tracks) {
        this.playlist = playlist;
        this.tracks = tracks;
    }

    @NonNull
    public Playlist getPlaylist() {
        return playlist;
    }

    public boolean isLikedByUser() {
        return playlist.isLikedByCurrentUser().or(false);
    }

    public Optional<Boolean> isMarkedForOffline() {
        return playlist.isMarkedForOffline();
    }

    public OfflineState getDownloadState() {
        return playlist.offlineState().or(OfflineState.NOT_OFFLINE);
    }

    public boolean isRepostedByUser() {
        return playlist.isRepostedByCurrentUser().or(false);
    }

    @Override
    public Urn getUrn() {
        return playlist.urn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return playlist.imageUrlTemplate();
    }

    public Urn getCreatorUrn() {
        return playlist.creatorUrn();
    }

    public String getCreatorName() {
        return playlist.creatorName();
    }

    public String getTitle() {
        return playlist.title();
    }

    public int getLikesCount() {
        return playlist.likesCount();
    }

    public int getRepostsCount() {
        return playlist.repostCount();
    }

    boolean isOwnedBy(Urn userUrn) {
        return userUrn.equals(getCreatorUrn());
    }

    @NotNull
    public List<TrackItem> getTracks() {
        return tracks;
    }

    public List<Urn> getTracksUrn() {
        return Lists.transform(tracks, PlayableItem.TO_URN);
    }

    public String getDuration() {
        final long duration = tracks.isEmpty() ?
                              playlist.duration() :
                              getCombinedTrackDurations();
        return ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS);
    }

    private long getCombinedTrackDurations() {
        long duration = 0;
        for (TrackItem track : tracks) {
            duration += track.getDuration();
        }
        return duration;
    }

    public int getTrackCount() {
        return tracks.isEmpty()
               ? playlist.trackCount()
               : tracks.size();
    }

    public boolean isPrivate() {
        return playlist.isPrivate();
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public String getPermalinkUrl() {
        return playlist.permalinkUrl();
    }

    @Override
    public PlaylistWithTracks updatedWithTrackCount(int trackCount) {
        return new PlaylistWithTracks(this.playlist.toBuilder().trackCount(trackCount).build(), tracks);
    }

    @Override
    public PlaylistWithTracks updatedWithMarkedForOffline(boolean markedForOffline) {
        return new PlaylistWithTracks(this.playlist.toBuilder().isMarkedForOffline(markedForOffline).build(), tracks);
    }

    @Override
    public PlaylistWithTracks updatedWithPlaylist(Playlist playlist) {
        return new PlaylistWithTracks(playlist, tracks);
    }

    PlaylistWithTracks updatedWithLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
        final Playlist.Builder builder = this.playlist.toBuilder().isLikedByCurrentUser(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            builder.likesCount(likeStatus.likeCount().get());
        }
        return new PlaylistWithTracks(builder.build(), tracks);
    }

    PlaylistWithTracks updatedWithRepostStatus(RepostsStatusEvent.RepostStatus repostStatus) {
        final Playlist.Builder builder = this.playlist.toBuilder().isRepostedByCurrentUser(repostStatus.isReposted());
        if (repostStatus.repostCount().isPresent()) {
            builder.repostCount(repostStatus.repostCount().get());
        }
        return new PlaylistWithTracks(builder.build(), tracks);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlaylistWithTracks)) {
            return false;
        }

        PlaylistWithTracks that = (PlaylistWithTracks) o;
        return playlist.equals(that.playlist) && tracks.equals(that.tracks);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(playlist, tracks);
    }

    @Override
    public String toString() {
        return "PlaylistWithTracks{" +
                "playlist=" + playlist +
                ", tracks=" + tracks +
                '}';
    }
}
