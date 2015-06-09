package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.likes.PlaylistLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistLikesPresenter extends ListPresenter<PlaylistItem> {

    private final PlaylistLikeOperations likeOperations;
    private final PlaylistLikesAdapter adapter;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    @Inject
    public PlaylistLikesPresenter(ImageOperations imageOperations,
                                  PullToRefreshWrapper pullToRefreshWrapper,
                                  PlaylistLikeOperations likeOperations,
                                  PlaylistLikesAdapter adapter,
                                  EventBus eventBus) {
        super(imageOperations, pullToRefreshWrapper);
        this.likeOperations = likeOperations;
        this.adapter = adapter;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_liked_playlists_message);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),

                likeOperations.onPlaylistLiked()
                        .map(PlaylistItem.fromPropertySet())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PrependItemToListSubscriber<>(adapter)),

                likeOperations.onPlaylistUnliked()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RemoveEntityListSubscriber(adapter)),

                eventBus.queue(EventQueue.CURRENT_DOWNLOAD)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateCurrentDownloadSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<PlaylistItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(likeOperations.likedPlaylists(), PlaylistItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<PlaylistItem> onRefreshBinding() {
        return CollectionBinding.from(likeOperations.updatedLikedPlaylists(), PlaylistItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    public void onItemClicked(View view, int position) {
        Urn playlistUrn = adapter.getItem(position).getEntityUrn();
        PlaylistDetailActivity.start(view.getContext(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }

    @Override
    protected int handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
