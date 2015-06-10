package com.soundcloud.android.playlists;

import com.soundcloud.android.presentation.PagingListItemAdapter;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingListItemAdapter<PlaylistItem> {

    @Inject
    public PlaylistLikesAdapter(DownloadablePlaylistItemRenderer playlistRenderer) {
        super(playlistRenderer);
    }
}
