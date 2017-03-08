package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import rx.Observable;

import android.support.annotation.Nullable;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class UserSoundsItemClickListener {

    @Nullable
    private static PlayableItem userSoundsItemToPlayableItem(final UserSoundsItem userSoundsItem) {
        return userSoundsItem.getPlayableItem().orNull();
    }

    private final Navigator navigator;
    private final MixedItemClickListener mixedItemClickListener;

    UserSoundsItemClickListener(Navigator navigator, MixedItemClickListener mixedItemClickListener) {
        this.navigator = navigator;
        this.mixedItemClickListener = mixedItemClickListener;
    }

    public void onItemClick(Observable<List<PlayableItem>> playables,
                            View view,
                            int position,
                            UserSoundsItem item,
                            Urn userUrn,
                            SearchQuerySourceInfo searchQuerySourceInfo,
                            Module module) {
        final int itemType = item.itemType();

        switch (itemType) {
            case UserSoundsItem.TYPE_VIEW_ALL:
                handleViewAllClickEvent(view, item, userUrn, searchQuerySourceInfo);
                break;
            case UserSoundsItem.TYPE_PLAYLIST:
                this.mixedItemClickListener.onPostClick(playables, view, position, userSoundsItemToPlayableItem(item), module);
                break;
            case UserSoundsItem.TYPE_TRACK:
                this.mixedItemClickListener.onProfilePostClick(playables,
                                                               view,
                                                               position,
                                                               userSoundsItemToPlayableItem(item),
                                                               userUrn);
                break;
            case UserSoundsItem.TYPE_HEADER:
            case UserSoundsItem.TYPE_DIVIDER:
            default:
                // do nothing, this is not an interactive item
        }
    }

    private void handleViewAllClickEvent(View view, UserSoundsItem item, Urn userUrn,
                                         SearchQuerySourceInfo searchQuerySourceInfo) {
        final int collectionType = item.collectionType();

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
                navigator.openProfilePlaylists(view.getContext(),
                                               userUrn,
                                               Screen.USER_PLAYLISTS,
                                               searchQuerySourceInfo);
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
            final MixedItemClickListener clickListener = this.mixedItemClickListenerFactory.create(Screen.USER_MAIN,
                                                                                                   searchQuerySourceInfo);
            return new UserSoundsItemClickListener(navigator, clickListener);
        }
    }
}
