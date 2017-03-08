package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.getPositionInModule;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.LayoutInflater;
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
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_user_sounds_playlist_row,
                                                                parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final UserSoundsItem userSoundsItem = items.get(position);
        final Optional<PlaylistItem> playlistItem = userSoundsItem.playlistItem();

        if (playlistItem.isPresent()) {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.white));
            playlistItemRenderer.bindPlaylistView(playlistItem.get(), itemView,
                                                  Optional.of(fromModule(userSoundsItem.collectionType(),
                                                                         getPositionInModule(items, userSoundsItem))));
        }
    }
}
