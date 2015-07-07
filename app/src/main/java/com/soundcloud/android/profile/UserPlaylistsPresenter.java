package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileArguments.USER_URN_KEY;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.rx.Pager;
import rx.Observable;

import android.os.Bundle;

import javax.inject.Inject;

class UserPlaylistsPresenter extends ProfilePlayablePresenter<PagedRemoteCollection> {

    private final UserProfileOperations profileOperations;

    @Inject
    UserPlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           ImagePauseOnScrollListener imagePauseOnScrollListener,
                           MixedPlayableRecyclerItemAdapter adapter,
                           MixedItemClickListener.Factory clickListenerFactory,
                           PlayableListUpdater.Factory updaterFactory,
                           UserProfileOperations profileOperations) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter,
                clickListenerFactory, updaterFactory);
        this.profileOperations = profileOperations;
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn userUrn = fragmentArgs.getParcelable(USER_URN_KEY);
        return CollectionBinding.from(getPagedObservable(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(getPagingFunction())
                .build();
    }


    protected Pager.PagingFunction<PagedRemoteCollection> getPagingFunction() {
        return profileOperations.playlistsPagingFunction();
    }

    protected Observable<PagedRemoteCollection> getPagedObservable(Urn userUrn) {
        return profileOperations.pagedPlaylists(userUrn);
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.new_empty_user_playlists_message);
        emptyView.setImage(R.drawable.empty_playlists);
    }
}
