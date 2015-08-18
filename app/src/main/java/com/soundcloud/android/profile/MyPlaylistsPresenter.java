package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class MyPlaylistsPresenter extends ProfilePlayablePresenter<List<PropertySet>> {

    private final MyProfileOperations profileOperations;

    @Inject
    MyPlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         ImagePauseOnScrollListener imagePauseOnScrollListener,
                         MixedPlayableRecyclerItemAdapter adapter,
                         MixedItemClickListener.Factory clickListenerFactory,
                         PlayableListUpdater.Factory updaterFactory,
                         MyProfileOperations profileOperations) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter,
                clickListenerFactory, updaterFactory);
        this.profileOperations = profileOperations;
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(profileOperations.pagedPlaylistItems(), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.playlistPagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<PlayableItem> onRefreshBinding() {
        return CollectionBinding.from(profileOperations.updatedPlaylists(), pageTransformer)
                .withAdapter(adapter)
                .withPager(profileOperations.postsPagingFunction())
                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.list_empty_user_playlists_message);
        emptyView.setImage(R.drawable.empty_sounds);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(adapter.getItems(), view, position);
    }
}
