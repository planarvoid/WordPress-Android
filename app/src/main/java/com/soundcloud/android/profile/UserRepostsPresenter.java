package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;

import android.os.Bundle;

import javax.inject.Inject;

class UserRepostsPresenter extends ProfilePlayablePresenter<PagedRemoteCollection<PlayableItem>> {

    private final UserProfileOperations operations;

    @Inject
    UserRepostsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
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
        return CollectionBinding.from(operations.userReposts(userUrn))
                                .withAdapter(adapter)
                                .withPager(operations.repostsPagingFunction())
                                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setImage(R.drawable.empty_stream);
        emptyView.setMessageText(R.string.user_reposts_empty);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
