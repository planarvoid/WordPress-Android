package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerViewAdapter;
import rx.Observable;

import javax.inject.Inject;

class UserPlaylistsPresenter extends ProfilePlayablePresenter {

    @Inject
    UserPlaylistsPresenter(PullToRefreshWrapper pullToRefreshWrapper,
                           RecyclerViewPauseOnScrollListener pauseOnScrollListener,
                           MixedPlayableRecyclerViewAdapter adapter,
                           MixedPlayableItemClickListener.Factory clickListenerFactory,
                           PlayableListUpdater.Factory updaterFactory,
                           ProfileOperations profileOperations) {
        super(pullToRefreshWrapper, pauseOnScrollListener, adapter, clickListenerFactory, updaterFactory, profileOperations);
    }

    @Override
    protected Pager.PagingFunction<PagedRemoteCollection> getPagingFunction() {
        return profileOperations.playlistsPagingFunction();
    }

    @Override
    protected Observable<PagedRemoteCollection> getPagedObservable(Urn userUrn) {
        return profileOperations.pagedPlaylists(userUrn);
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        getEmptyView().setMessageText(R.string.list_empty_user_playlists_message);
        getEmptyView().setImage(R.drawable.empty_playlists);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
