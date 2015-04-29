package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.likes.PlaylistLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;

import javax.inject.Inject;

public class PlaylistLikesPresenter extends ListPresenter<PropertySet, PlaylistItem>
        implements AdapterView.OnItemClickListener {

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
        getListBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getListView().setOnItemClickListener(this);
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

                eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new UpdateCurrentDownloadSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected ListBinding<PropertySet, PlaylistItem> onBuildListBinding() {
        return ListBinding.pagedList(
                likeOperations.likedPlaylists(),
                adapter,
                likeOperations.likedPlaylistsPager(),
                PlaylistItem.fromPropertySets());
    }

    @Override
    protected ListBinding<PropertySet, PlaylistItem> onBuildRefreshBinding() {
        return ListBinding.pagedList(
                likeOperations.updatedLikedPlaylists(),
                adapter,
                likeOperations.likedPlaylistsPager(),
                PlaylistItem.fromPropertySets());
    }

    @Override
    protected void onSubscribeListBinding(ListBinding<PropertySet, PlaylistItem> listBinding) {
        // No-op
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Urn playlistUrn = adapter.getItem(position).getEntityUrn();
        PlaylistDetailActivity.start(adapterView.getContext(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }
}
