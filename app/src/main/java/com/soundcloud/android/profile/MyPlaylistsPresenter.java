package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class MyPlaylistsPresenter extends ProfilePlayablePresenter<List<PropertySet>> {

    private final Action1<List<PropertySet>> clearAdapterOnNext = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            adapter.clear();
        }
    };

    private final MyProfileOperations profileOperations;
    private final EventBus eventBus;
    private Subscription playlistDeletedSubscription = RxUtils.invalidSubscription();

    @Inject
    MyPlaylistsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         ImagePauseOnScrollListener imagePauseOnScrollListener,
                         MixedPlayableRecyclerItemAdapter adapter,
                         MixedItemClickListener.Factory clickListenerFactory,
                         PlayableListUpdater.Factory updaterFactory,
                         MyProfileOperations profileOperations, EventBus eventBus) {
        super(swipeRefreshAttacher, imagePauseOnScrollListener, adapter,
              clickListenerFactory, updaterFactory);
        this.profileOperations = profileOperations;
        this.eventBus = eventBus;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        playlistDeletedSubscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                                              .filter(EntityStateChangedEvent.IS_PLAYLIST_DELETED)
                                              .observeOn(AndroidSchedulers.mainThread())
                                              .subscribe(new RefreshSubscriber());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistDeletedSubscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<List<PropertySet>, PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(profileOperations.pagedPlaylistItems(), pageTransformer)
                                .withAdapter(adapter)
                                .withPager(profileOperations.playlistPagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<PropertySet>, PlayableItem> onRefreshBinding() {
        return CollectionBinding.from(profileOperations.updatedPlaylists(), pageTransformer)
                                .withAdapter(adapter)
                                .withPager(profileOperations.playlistPagingFunction())
                                .build();
    }

    @Override
    protected void configureEmptyView(EmptyView emptyView) {
        emptyView.setMessageText(R.string.list_empty_you_playlists_message);
        emptyView.setImage(R.drawable.empty_playlists);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        clickListener.onItemClick(adapter.getItems(), view, position);
    }

    private class RefreshSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent ignored) {
            final Observable<List<PropertySet>> collectionObservable = profileOperations
                    .pagedPlaylistItems()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(clearAdapterOnNext);

            retryWith(CollectionBinding.from(collectionObservable, pageTransformer)
                                       .withAdapter(adapter)
                                       .withPager(profileOperations.playlistPagingFunction())
                                       .build());
        }
    }
}
