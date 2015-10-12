package com.soundcloud.android.search;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.playlists.ApiPlaylistCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class PlaylistResultsPresenter extends RecyclerViewPresenter<PlaylistItem> {

    private final PlaylistDiscoveryOperations operations;
    private final PlaylistResultsAdapter adapter;
    private final Navigator navigator;
    private final EventBus eventBus;

    @Inject
    PlaylistResultsPresenter(
            PlaylistDiscoveryOperations operations,
            PlaylistResultsAdapter adapter,
            SwipeRefreshAttacher swipeRefreshAttacher,
            Navigator navigator,
            EventBus eventBus) {
        super(swipeRefreshAttacher, Options.grid(R.integer.grid_view_num_columns).build());
        this.operations = operations;
        this.adapter = adapter;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_PLAYLIST_DISCO));
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        new EmptyViewBuilder().configureForSearch(getEmptyView());
    }

    @Override
    protected CollectionBinding<PlaylistItem> onBuildBinding(Bundle fragmentArgs) {
        String playlistTag = fragmentArgs.getString(PlaylistResultsFragment.KEY_PLAYLIST_TAG);
        return CollectionBinding.from(
                operations.playlistsForTag(playlistTag),
                PlaylistItem.<ApiPlaylistCollection>fromApiPlaylists())
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
        navigator.openPlaylist(view.getContext(), playlist.getEntityUrn(), Screen.SEARCH_PLAYLIST_DISCO);
        eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO));
    }
}
