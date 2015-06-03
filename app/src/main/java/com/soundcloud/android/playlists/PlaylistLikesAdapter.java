package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.PagingItemAdapter;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingItemAdapter<PlaylistItem> {

    @Inject
    public PlaylistLikesAdapter(DownloadablePlaylistItemRenderer playlistRenderer) {
        super(playlistRenderer);
    }
}
