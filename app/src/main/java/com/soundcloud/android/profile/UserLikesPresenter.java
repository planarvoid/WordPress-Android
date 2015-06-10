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

class UserLikesPresenter extends ProfilePlayablePresenter {

    @Inject
    UserLikesPresenter(PullToRefreshWrapper pullToRefreshWrapper,
                       RecyclerViewPauseOnScrollListener pauseOnScrollListener,
                       MixedPlayableRecyclerViewAdapter adapter,
                       MixedPlayableItemClickListener.Factory clickListenerFactory,
                       PlayableListUpdater.Factory updaterFactory,
                       ProfileOperations profileOperations) {
        super(pullToRefreshWrapper, pauseOnScrollListener, adapter, clickListenerFactory, updaterFactory, profileOperations);
    }

    @Override
    protected Pager.PagingFunction<PagedRemoteCollection> getPagingFunction() {
        return profileOperations.likesPagingFunction();
    }

    @Override
    protected Observable<PagedRemoteCollection> getPagedObservable(Urn userUrn) {
        return profileOperations.pagedLikes(userUrn);
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        getEmptyView().setMessageText(R.string.new_empty_user_likes_text);
        getEmptyView().setImage(R.drawable.empty_like);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
