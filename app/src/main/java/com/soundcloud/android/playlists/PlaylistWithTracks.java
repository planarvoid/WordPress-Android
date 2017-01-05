package com.soundcloud.android.playlists;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistWithTracks implements ImageResource {

    @NotNull private final PlaylistItem playlistItem;
    @NotNull private final List<TrackItem> tracks;

    public PlaylistWithTracks(@NotNull PlaylistItem playlistItem, @NotNull List<TrackItem> tracks) {
        this.playlistItem = playlistItem;
        this.tracks = tracks;
    }

    @NonNull
    public PlaylistItem getPlaylistItem() {
        return playlistItem;
    }

    public boolean isLikedByUser() {
        return playlistItem.isLikedByCurrentUser();
    }

    public Optional<Boolean> isMarkedForOffline() {
        return playlistItem.isMarkedForOffline();
    }

    public OfflineState getDownloadState() {
        return playlistItem.getDownloadState();
    }

    public void setOfflineState(OfflineState offlineState) {
        playlistItem.setOfflineState(offlineState);
    }

    public boolean isRepostedByUser() {
        return playlistItem.isRepostedByCurrentUser();
    }

    boolean isLocalPlaylist() {
        return playlistItem.isLocalPlaylist();
    }

    boolean needsTracks() {
        return getTracks().isEmpty();
    }

    @Override
    public Urn getUrn() {
        return playlistItem.getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return playlistItem.getImageUrlTemplate();
    }

    public Urn getCreatorUrn() {
        return playlistItem.getCreatorUrn();
    }

    public String getCreatorName() {
        return playlistItem.getCreatorName();
    }

    public String getTitle() {
        return playlistItem.getTitle();
    }

    public int getLikesCount() {
        return playlistItem.getLikesCount();
    }

    public int getRepostsCount() {
        return playlistItem.getRepostCount();
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
                              playlistItem.getDuration() :
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
               ? playlistItem.getTrackCount()
               : tracks.size();
    }

    public boolean isPrivate() {
        return playlistItem.isPrivate();
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public String getPermalinkUrl() {
        return playlistItem.getPermalinkUrl();
    }

    public PlaylistWithTracks update(PropertySet source) {
        return new PlaylistWithTracks(this.playlistItem.updated(source), tracks);
    }

    public PlaylistWithTracks updatedWithLikeStatus(LikesStatusEvent.LikeStatus likeStatus) {
        return new PlaylistWithTracks(this.playlistItem.updatedWithLike(likeStatus), tracks);
    }

    public PlaylistWithTracks updatedWithRepostStatus(RepostsStatusEvent.RepostStatus repostStatus) {
        return new PlaylistWithTracks(this.playlistItem.updatedWithRepost(repostStatus), tracks);
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
        return playlistItem.equals(that.playlistItem) && tracks.equals(that.tracks);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(playlistItem, tracks);
    }

    @Override
    public String toString() {
        return "PlaylistWithTracks{" +
                "playlistItem=" + playlistItem +
                ", tracks=" + tracks +
                '}';
    }
}
