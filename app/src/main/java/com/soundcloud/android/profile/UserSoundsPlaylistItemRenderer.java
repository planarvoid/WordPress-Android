package com.soundcloud.android.profile;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsPlaylistItemRenderer implements CellRenderer<UserSoundsItem> {
    private final PlaylistItemRenderer playlistItemRenderer;

    @Inject
    public UserSoundsPlaylistItemRenderer(PlaylistItemRenderer playlistItemRenderer) {
        this.playlistItemRenderer = playlistItemRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final Optional<PlaylistItem> playlistItem = items.get(position).getPlaylistItem();

        if (playlistItem.isPresent()) {
            playlistItemRenderer.bindPlaylistView(playlistItem.get(), itemView);
        }
    }
}
