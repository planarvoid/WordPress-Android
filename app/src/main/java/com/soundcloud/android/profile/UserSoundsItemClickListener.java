package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import rx.Observable;

import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class UserSoundsItemClickListener {

    private static final Function<PlaylistItem, PlayableItem> PLAYLIST_ITEM_TO_PLAYABLE_ITEM = new Function<PlaylistItem, PlayableItem>() {
        @Override
        public PlayableItem apply(final PlaylistItem playlistItem) {
            return PlayableItem.from(playlistItem.getSource());
        }
    };

    private static final Function<TrackItem, PlayableItem> TRACK_ITEM_TO_PLAYABLE_ITEM = new Function<TrackItem, PlayableItem>() {
        @Override
        public PlayableItem apply(final TrackItem trackItem) {
            return PlayableItem.from(trackItem.getSource());
        }
    };

    private static PlayableItem userSoundsItemToPlayableItem(final UserSoundsItem userSoundsItem) {
        return userSoundsItem.getPlaylistItem()
                .transform(PLAYLIST_ITEM_TO_PLAYABLE_ITEM)
                .or(userSoundsItem.getTrackItem().transform(TRACK_ITEM_TO_PLAYABLE_ITEM))
                .orNull();
    }

    private final Navigator navigator;
    private final MixedItemClickListener mixedItemClickListener;

    UserSoundsItemClickListener(Navigator navigator, MixedItemClickListener mixedItemClickListener) {
        this.navigator = navigator;
        this.mixedItemClickListener = mixedItemClickListener;
    }

    public void onItemClick(Observable<List<PropertySet>> playables, View view, int position, UserSoundsItem item, Urn userUrn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final int itemType = item.getItemType();

        switch (itemType) {
            case UserSoundsItem.TYPE_VIEW_ALL:
                handleViewAllClickEvent(view, item, userUrn, searchQuerySourceInfo);
                break;
            case UserSoundsItem.TYPE_PLAYLIST:
            case UserSoundsItem.TYPE_TRACK:
                this.mixedItemClickListener.onPostClick(playables, view, position, userSoundsItemToPlayableItem(item));
                break;
            case UserSoundsItem.TYPE_HEADER:
            case UserSoundsItem.TYPE_DIVIDER:
            default:
                // do nothing, this is not an interactive item
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

    public static class Factory {
        private final Navigator navigator;
        private final MixedItemClickListener.Factory mixedItemClickListenerFactory;

        @Inject
        Factory(Navigator navigator, MixedItemClickListener.Factory mixedItemClickListenerFactory) {
            this.navigator = navigator;
            this.mixedItemClickListenerFactory = mixedItemClickListenerFactory;
        }

        public UserSoundsItemClickListener create(SearchQuerySourceInfo searchQuerySourceInfo) {
            final MixedItemClickListener clickListener = this.mixedItemClickListenerFactory.create(Screen.PROFILE_SOUNDS_PLAYLIST, searchQuerySourceInfo);
            return new UserSoundsItemClickListener(navigator, clickListener);
        }
    }
}
