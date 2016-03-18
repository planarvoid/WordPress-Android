package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;

import android.view.View;

import javax.inject.Inject;

public class UserSoundsItemClickListener {

    private final Navigator navigator;

    @Inject
    public UserSoundsItemClickListener(Navigator navigator) {
        this.navigator = navigator;
    }

    public void onItemClick(View view, UserSoundsItem item) {
        // In the future, this method should gather the playables from the list of items in the adapter
        // and forward them to the mixedItemClickListener.
        // Note: The mixed item click listener may need additional love to play through both tracks and playlists, as
        // that is now supported by the playback functionality.
        // mixedItemClickListener.onItemClick(Collections.<ListItem>emptyList(), view, position);

        switch (item.getItemType()) {
            case UserSoundsItem.TYPE_PLAYLIST:
                PlaylistItem playlist = item.getPlaylistItem().get();
                navigator.openPlaylist(view.getContext(), playlist.getUrn(), Screen.PROFILE_SOUNDS_PLAYLIST);
            default:
        }
    }
}
