package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;

import android.view.View;

import javax.inject.Inject;

public class UserSoundsItemClickListener {

    private final Navigator navigator;

    @Inject
    public UserSoundsItemClickListener(Navigator navigator) {
        this.navigator = navigator;
    }

    public void onItemClick(View view, UserSoundsItem item, Urn userUrn, SearchQuerySourceInfo searchQuerySourceInfo) {
        // In the future, this method should gather the playables from the list of items in the adapter
        // and forward them to the mixedItemClickListener.
        // Note: The mixed item click listener may need additional love to play through both tracks and playlists, as
        // that is now supported by the playback functionality.
        // mixedItemClickListener.onItemClick(Collections.<ListItem>emptyList(), view, position);
        final int itemType = item.getItemType();

        switch (itemType) {
            case UserSoundsItem.TYPE_PLAYLIST:
                PlaylistItem playlist = item.getPlaylistItem().get();
                navigator.openPlaylist(view.getContext(), playlist.getUrn(), Screen.PROFILE_SOUNDS_PLAYLIST);
                break;
            case UserSoundsItem.TYPE_VIEW_ALL:
                handleViewAllClickEvent(view, item, userUrn, searchQuerySourceInfo);
                break;
            default:
                throw new IllegalArgumentException("Unknown item type : " + itemType);
        }
    }

    private void handleViewAllClickEvent(View view, UserSoundsItem item, Urn userUrn,
                                         SearchQuerySourceInfo searchQuerySourceInfo) {
        final int collectionType = item.getCollectionType();

        switch (collectionType) {
            case UserSoundsTypes.REPOSTS:
                navigator.openProfileReposts(view.getContext(), userUrn, Screen.USERS_REPOSTS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.TRACKS:
                navigator.openProfileTracks(view.getContext(), userUrn, Screen.USER_TRACKS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.ALBUMS:
                navigator.openProfileAlbums(view.getContext(), userUrn, Screen.USER_TRACKS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.LIKES:
                navigator.openProfileLikes(view.getContext(), userUrn, Screen.USER_LIKES, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.PLAYLISTS:
                navigator.openProfilePlaylists(view.getContext(), userUrn, Screen.USER_PLAYLISTS, searchQuerySourceInfo);
                break;
            default:
                throw new IllegalArgumentException("Unknown collection type : " + collectionType);
        }
    }
}
