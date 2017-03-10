package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;

import java.util.Date;

@AutoValue
public abstract class PlaylistStreamItem extends StreamItem implements LikeableItem, RepostableItem, UpdatablePlaylistItem {
    public abstract PlaylistItem playlistItem();
    public abstract boolean promoted();
    public abstract Date createdAt();

    static PlaylistStreamItem create(PlaylistItem playlistItem, Date createdAt) {
        return new AutoValue_PlaylistStreamItem(Kind.PLAYLIST, playlistItem, playlistItem.isPromoted(), createdAt);
    }

    private PlaylistStreamItem create(PlaylistItem playlistItem) {
        return new AutoValue_PlaylistStreamItem(Kind.PLAYLIST, playlistItem, promoted(), createdAt());
    }

    @Override
    public Urn getUrn() {
        return playlistItem().getUrn();
    }

    @Override
    public PlaylistStreamItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return create(playlistItem().updatedWithLike(likeStatus));
    }

    @Override
    public PlaylistStreamItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return create(playlistItem().updatedWithRepost(repostStatus));
    }

    @Override
    public PlaylistStreamItem updatedWithPlaylist(Playlist playlist) {
        return create(playlistItem().toBuilder().playlist(playlist).build());
    }

    @Override
    public PlaylistStreamItem updatedWithTrackCount(int trackCount) {
        return create(playlistItem().updatedWithTrackCount(trackCount));
    }

    @Override
    public PlaylistStreamItem updatedWithMarkedForOffline(boolean markedForOffline) {
        return create(playlistItem().updatedWithMarkedForOffline(markedForOffline));
    }
}
