package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistPostsPresenter extends RecyclerViewPresenter<PlaylistItem> {

    private final PlaylistItemAdapter adapter;
    private final PlaylistPostOperations playlistPostOperations;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Subscription eventSubscriptions = RxUtils.invalidSubscription();

    @Inject
    PlaylistPostsPresenter(PlaylistItemAdapter adapter,
                           PlaylistPostOperations playlistPostOperations,
                           SwipeRefreshAttacher swipeRefreshAttacher,
                           Navigator navigator, EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.adapter = adapter;
        this.playlistPostOperations = playlistPostOperations;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<PlaylistItem> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(
                playlistPostOperations.postedPlaylists(), PlaylistItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(playlistPostOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<PlaylistItem> onRefreshBinding() {
        return CollectionBinding.from(
                playlistPostOperations.updatedPostedPlaylists(), PlaylistItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(playlistPostOperations.pagingFunction())
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getEmptyView().setImage(R.drawable.empty_playlists);
        getEmptyView().setMessageText(R.string.list_empty_you_playlists_message);

        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new UpdateCurrentDownloadSubscriber(adapter))
        );
    }

    @Override
    protected EmptyView.Status handleError(Throwable throwable) {
        return ErrorUtils.emptyViewStatusFromError(throwable);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onItemClicked(View view, int position) {
        Urn playlistUrn = adapter.getItem(position).getEntityUrn();
        navigator.openPlaylist(view.getContext(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }
}
