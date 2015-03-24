package com.soundcloud.android.playlists;

import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;

import javax.inject.Inject;

public class PagedPlaylistsAdapter extends PagingItemAdapter<PlaylistItem> {

    private final PlaylistItemPresenter playlistPresenter;

    @Inject
    PagedPlaylistsAdapter(PlaylistItemPresenter playlistPresenter) {
        super(playlistPresenter);
        this.playlistPresenter = playlistPresenter;
    }

    PlaylistItemPresenter getPlaylistPresenter() {
        return playlistPresenter;
    }

}
