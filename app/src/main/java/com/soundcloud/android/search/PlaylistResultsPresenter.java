package com.soundcloud.android.search;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class PlaylistResultsPresenter extends RecyclerViewPresenter<SearchResult, PlaylistItem> {

    private static final Func1<SearchResult, List<PlaylistItem>> TO_PRESENTATION_MODELS = new Func1<SearchResult, List<PlaylistItem>>() {
        @Override
        public List<PlaylistItem> call(SearchResult propertySets) {
            final List<PlaylistItem> result = new ArrayList<>(propertySets.getItems().size());
            for (PropertySet source : propertySets) {
                result.add(PlaylistItem.from(source));
            }
            return result;
        }
    };

    private final PlaylistDiscoveryOperations operations;
    private final PlaylistResultsAdapter adapter;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Subscription eventSubscription = RxUtils.invalidSubscription();

    @Inject
    PlaylistResultsPresenter(PlaylistDiscoveryOperations operations,
                             PlaylistResultsAdapter adapter,
                             SwipeRefreshAttacher swipeRefreshAttacher,
                             Navigator navigator,
                             EventBus eventBus) {
        super(swipeRefreshAttacher, Options.grid(R.integer.grids_num_columns).build());
        this.operations = operations;
        this.adapter = adapter;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        new EmptyViewBuilder().configureForSearch(getEmptyView());
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());

        eventSubscription = new CompositeSubscription(eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                                                      eventBus.subscribe(EventQueue.LIKE_CHANGED, new LikeEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected CollectionBinding<SearchResult, PlaylistItem> onBuildBinding(Bundle fragmentArgs) {
        String playlistTag = fragmentArgs.getString(PlaylistResultsFragment.KEY_PLAYLIST_TAG);
        return CollectionBinding.from(operations.playlistsForTag(playlistTag), TO_PRESENTATION_MODELS)
                                .withAdapter(adapter)
                                .withPager(operations.pager(playlistTag))
                                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        PlaylistItem playlist = adapter.getItem(position);
        navigator.legacyOpenPlaylist(view.getContext(), playlist.getUrn(), Screen.SEARCH_PLAYLIST_DISCO);
        eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO));
    }
}
