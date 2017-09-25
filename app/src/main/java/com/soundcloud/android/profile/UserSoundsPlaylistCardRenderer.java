package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.UserSoundsItem.getPositionInModule;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.view.adapters.PlaylistCardRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UserSoundsPlaylistCardRenderer extends UserSoundsItemRenderer {
    private final PlaylistCardRenderer playlistCardRenderer;

    @Inject
    public UserSoundsPlaylistCardRenderer(PlaylistCardRenderer playlistCardRenderer) {
        this.playlistCardRenderer = playlistCardRenderer;
        playlistCardRenderer.setLayoutResource(R.layout.profile_user_sounds_playlist_card);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return playlistCardRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final UserSoundsItem userSoundsItem = items.get(position);
        final Optional<PlaylistItem> playlistItemOptional = userSoundsItem.playlistItem();

        if (playlistItemOptional.isPresent()) {
            itemView.setBackgroundColor(itemView.getResources().getColor(R.color.white));
            final PlaylistItem playlistItem = playlistItemOptional.get();
            playlistCardRenderer.bindPlaylistCardView(playlistItem,
                                                      itemView,
                                                      Optional.of(fromModule(userSoundsItem.collectionType(), getPositionInModule(items, userSoundsItem))),
                                                      createItemMenuOptions(userSoundsItem, playlistItem));
        }
    }

}
