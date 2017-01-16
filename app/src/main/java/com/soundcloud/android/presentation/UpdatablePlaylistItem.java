package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.playlists.Playlist;

public interface UpdatablePlaylistItem extends Entity {
    UpdatablePlaylistItem updatedWithTrackCount(int trackCount);

    UpdatablePlaylistItem updatedWithMarkedForOffline(boolean markedForOffline);

    UpdatablePlaylistItem updatedWithPlaylist(Playlist playlist);
}
