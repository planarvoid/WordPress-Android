package com.soundcloud.android.profile;

import com.soundcloud.android.navigation.NavigationExecutor;
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

    private final NavigationExecutor navigationExecutor;
    private final MixedItemClickListener mixedItemClickListener;

    UserSoundsItemClickListener(NavigationExecutor navigationExecutor, MixedItemClickListener mixedItemClickListener) {
        this.navigationExecutor = navigationExecutor;
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
                navigationExecutor.openProfileReposts(view.getContext(), userUrn, Screen.USERS_REPOSTS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.TRACKS:
                navigationExecutor.openProfileTracks(view.getContext(), userUrn, Screen.USER_TRACKS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.ALBUMS:
                navigationExecutor.openProfileAlbums(view.getContext(), userUrn, Screen.USER_TRACKS, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.LIKES:
                navigationExecutor.openProfileLikes(view.getContext(), userUrn, Screen.USER_LIKES, searchQuerySourceInfo);
                break;
            case UserSoundsTypes.PLAYLISTS:
                navigationExecutor.openProfilePlaylists(view.getContext(),
                                                        userUrn,
                                                        Screen.USER_PLAYLISTS,
                                                        searchQuerySourceInfo);
                break;
            default:
                throw new IllegalArgumentException("Unknown collection type : " + collectionType);
        }
    }

    public static class Factory {
        private final NavigationExecutor navigationExecutor;
        private final MixedItemClickListener.Factory mixedItemClickListenerFactory;

        @Inject
        Factory(NavigationExecutor navigationExecutor, MixedItemClickListener.Factory mixedItemClickListenerFactory) {
            this.navigationExecutor = navigationExecutor;
            this.mixedItemClickListenerFactory = mixedItemClickListenerFactory;
        }

        public UserSoundsItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            final MixedItemClickListener clickListener = this.mixedItemClickListenerFactory.create(screen, searchQuerySourceInfo);
            return new UserSoundsItemClickListener(navigationExecutor, clickListener);
        }
    }
}
