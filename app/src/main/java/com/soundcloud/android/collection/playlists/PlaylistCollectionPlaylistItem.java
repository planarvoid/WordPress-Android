package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class PlaylistCollectionPlaylistItem extends PlaylistCollectionItem implements OfflineItem, UpdatablePlaylistItem, LikeableItem, RepostableItem {

    public static PlaylistCollectionPlaylistItem create(PlaylistItem playlistItem) {
        return new AutoValue_PlaylistCollectionPlaylistItem(PlaylistCollectionItem.TYPE_PLAYLIST, playlistItem);
    }

    abstract PlaylistItem getPlaylistItem();

    @Override
    public boolean isSingleSpan() {
        return true;
    }

    @Override
    public PlaylistCollectionPlaylistItem updatedWithTrackCount(int trackCount) {
        return create(getPlaylistItem().updatedWithTrackCount(trackCount));
    }

    @Override
    public PlaylistCollectionPlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        return create(getPlaylistItem().updatedWithMarkedForOffline(markedForOffline));
    }

    @Override
    public PlaylistCollectionPlaylistItem updatedWithOfflineState(OfflineState offlineState) {
        return create(getPlaylistItem().updatedWithOfflineState(offlineState));
    }

    public PlaylistCollectionPlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return create(getPlaylistItem().updatedWithLike(likeStatus));
    }

    public PlaylistCollectionPlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return create(getPlaylistItem().updatedWithRepost(repostStatus));
    }

    @Override
    public UpdatablePlaylistItem updatedWithPlaylistItem(PlaylistItem playlistItem) {
        return create(playlistItem);
    }

    @Override
    public Urn getUrn() {
        return getPlaylistItem().getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return getPlaylistItem().getImageUrlTemplate();
    }

}
