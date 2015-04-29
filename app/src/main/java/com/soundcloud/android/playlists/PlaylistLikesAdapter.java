package com.soundcloud.android.playlists;

import com.soundcloud.android.view.adapters.PagingItemAdapter;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingItemAdapter<PlaylistItem> {

    @Inject
    public PlaylistLikesAdapter(DownloadablePlaylistItemPresenter playlistPresenter) {
        super(playlistPresenter);
    }
}
