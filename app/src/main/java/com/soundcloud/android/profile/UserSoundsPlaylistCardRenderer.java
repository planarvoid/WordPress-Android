package com.soundcloud.android.profile;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.PlaylistCardRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class UserSoundsPlaylistCardRenderer implements CellRenderer<UserSoundsItem> {
    private final PlaylistCardRenderer playlistCardRenderer;

    @Inject
    public UserSoundsPlaylistCardRenderer(PlaylistCardRenderer playlistCardRenderer) {
        this.playlistCardRenderer = playlistCardRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistCardRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final Optional<PlaylistItem> playlistItem = items.get(position).getPlaylistItem();

        if (playlistItem.isPresent()) {
            playlistCardRenderer.bindPlaylistCardView(playlistItem.get(), itemView);
        }
    }
}
