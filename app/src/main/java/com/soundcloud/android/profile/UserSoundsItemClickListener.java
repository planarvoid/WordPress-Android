package com.soundcloud.android.profile;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import android.support.annotation.Nullable;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class UserSoundsItemClickListener {

    private final MixedItemClickListener mixedItemClickListener;
    private final Navigator navigator;

    @Nullable
    private static PlayableItem userSoundsItemToPlayableItem(final UserSoundsItem userSoundsItem) {
        return userSoundsItem.getPlayableItem().orNull();
    }

    UserSoundsItemClickListener(Navigator navigator, MixedItemClickListener mixedItemClickListener) {
        this.mixedItemClickListener = mixedItemClickListener;
        this.navigator = navigator;
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

    private void handleViewAllClickEvent(View view, UserSoundsItem item, Urn userUrn, @Nullable SearchQuerySourceInfo searchQuerySourceInfo) {
        final int collectionType = item.collectionType();

        switch (collectionType) {
            case UserSoundsTypes.REPOSTS:
                navigator.navigateTo(NavigationTarget.forProfileReposts(getFragmentActivity(view), userUrn, Optional.fromNullable(searchQuerySourceInfo)));
                break;
            case UserSoundsTypes.TRACKS:
                navigator.navigateTo(NavigationTarget.forProfileTracks(getFragmentActivity(view), userUrn, Optional.fromNullable(searchQuerySourceInfo)));
                break;
            case UserSoundsTypes.ALBUMS:
                navigator.navigateTo(NavigationTarget.forProfileAlbums(getFragmentActivity(view), userUrn, Optional.fromNullable(searchQuerySourceInfo)));
                break;
            case UserSoundsTypes.LIKES:
                navigator.navigateTo(NavigationTarget.forProfileLikes(getFragmentActivity(view), userUrn, Optional.fromNullable(searchQuerySourceInfo)));
                break;
            case UserSoundsTypes.PLAYLISTS:
                navigator.navigateTo(NavigationTarget.forProfilePlaylists(getFragmentActivity(view), userUrn, Optional.fromNullable(searchQuerySourceInfo)));
                break;
            default:
                throw new IllegalArgumentException("Unknown collection type : " + collectionType);
        }
    }

    public static class Factory {
        private final Navigator navigator;
        private final MixedItemClickListener.Factory mixedItemClickListenerFactory;

        @Inject
        Factory(Navigator navigationExecutor, MixedItemClickListener.Factory mixedItemClickListenerFactory) {
            this.navigator = navigationExecutor;
            this.mixedItemClickListenerFactory = mixedItemClickListenerFactory;
        }

        public UserSoundsItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            final MixedItemClickListener clickListener = this.mixedItemClickListenerFactory.create(screen, searchQuerySourceInfo);
            return new UserSoundsItemClickListener(navigator, clickListener);
        }
    }
}
