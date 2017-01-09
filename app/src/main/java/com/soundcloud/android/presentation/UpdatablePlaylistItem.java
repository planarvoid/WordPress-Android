package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.playlists.PlaylistItem;

public interface UpdatablePlaylistItem extends Entity {
    UpdatablePlaylistItem updatedWithTrackCount(int trackCount);

    UpdatablePlaylistItem updatedWithMarkedForOffline(boolean markedForOffline);

    UpdatablePlaylistItem updatedWithPlaylistItem(PlaylistItem playlistItem);
}
