package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import rx.functions.Func1;

import android.os.Bundle;

import javax.inject.Inject;

class UserPlaylistsPresenter extends ProfilePlayablePresenter<PagedRemoteCollection<PlayableItem>> {
    private static final Func1<PagedRemoteCollection<PlaylistItem>, PagedRemoteCollection<PlayableItem>> CAST_AS_PLAYABLE = playlists -> playlists.transform(playlist -> (PlayableItem) playlist);
    private final UserProfileOperations operations;

    @Inject
    UserPlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           ImagePauseOnScrollListener imagePauseOnScrollListener,
                           MixedPlayableRecyclerItemAdapter adapter,
                           MixedItemClickListener.Factory clickListenerFactory,
                           PlayableListUpdater.Factory updaterFactory,
                           UserProfileOperations operations) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter,
              clickListenerFactory, updaterFactory);
        this.operations = operations;
    }

    @Override
    protected CollectionBinding<PagedRemoteCollection<PlayableItem>, PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(ProfileArguments.USER_URN_KEY);
        return CollectionBinding.from(operations.userPlaylists(userUrn).map(CAST_AS_PLAYABLE))
                                .withAdapter(adapter)
                                .withPager(operations.pagingFunction(nextPage -> operations.userPlaylists(nextPage).map(CAST_AS_PLAYABLE)))
                                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setImage(R.drawable.empty_playlists);
        emptyView.setMessageText(R.string.user_profile_sounds_playlists_empty);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
