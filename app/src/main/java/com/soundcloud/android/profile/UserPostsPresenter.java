package com.soundcloud.android.profile;

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

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

class UserPostsPresenter extends ProfilePlayablePresenter<PagedRemoteCollection> {

    private final UserProfileOperations profileOperations;
    private Urn userUrn = Urn.NOT_SET;

    @Inject
    UserPostsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                       PostsRecyclerItemAdapter adapter,
                       MixedItemClickListener.Factory clickListenerFactory,
                       PlayableListUpdater.Factory updaterFactory,
                       UserProfileOperations profileOperations) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter,
                clickListenerFactory, updaterFactory);
        this.profileOperations = profileOperations;
    }

    @Override
    protected CollectionBinding<PagedRemoteCollection, PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        userUrn = fragmentArgs.getParcelable(UserPostsFragment.USER_URN_KEY);
        return CollectionBinding.from(profileOperations.pagedPostItems(userUrn), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.postsPagingFunction(userUrn))
                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.new_empty_user_posts_message);
        emptyView.setImage(R.drawable.empty_sounds);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        PlayableItem item = adapter.getItem(position);
        clickListener.onProfilePostClick(profileOperations.postsForPlayback(adapter.getItems()),
                view, position, item, userUrn);
    }
}
