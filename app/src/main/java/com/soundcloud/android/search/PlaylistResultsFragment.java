package com.soundcloud.android.search;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.DefaultFragment;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class PlaylistResultsFragment extends DefaultFragment
        implements ReactiveListComponent<ConnectableObservable<ModelCollection<ApiPlaylist>>> {

    public static final String TAG = "playlist_results";
    static final String KEY_PLAYLIST_TAG = "playlist_tag";

    @Inject PlaylistDiscoveryOperations operations;
    @Inject ListViewController listViewController;
    @Inject EndlessAdapter<ApiPlaylist> adapter;
    @Inject EventBus eventBus;

    private ConnectableObservable<ModelCollection<ApiPlaylist>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private PlaylistDiscoveryOperations.PlaylistPager pager;

    public static PlaylistResultsFragment newInstance(String tag) {
        PlaylistResultsFragment fragment = new PlaylistResultsFragment();
        Bundle arguments = new Bundle();
        arguments.putString(KEY_PLAYLIST_TAG, tag);
        fragment.setArguments(arguments);
        return fragment;
    }

    public PlaylistResultsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    PlaylistResultsFragment(PlaylistDiscoveryOperations operations, ListViewController listViewController,
                            EndlessAdapter<ApiPlaylist> adapter, EventBus eventBus) {
        this.operations = operations;
        this.listViewController = listViewController;
        this.adapter = adapter;
        this.eventBus = eventBus;
    }

    private void addLifeCycleComponents() {
        listViewController.setScrollListener(new AbsListViewParallaxer(null));
        addLifeCycleComponent(listViewController);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String playlistTag = getArguments().getString(KEY_PLAYLIST_TAG);
        this.pager = operations.pager(playlistTag);
        listViewController.setAdapter(adapter, pager);

        eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_PLAYLIST_DISCO.get());
        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<ModelCollection<ApiPlaylist>> buildObservable() {
        String playlistTag = getArguments().getString(KEY_PLAYLIST_TAG);
        return pager.page(operations.playlistsForTag(playlistTag)).observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<ModelCollection<ApiPlaylist>> observable) {
        this.observable = observable;
        this.observable.subscribe(adapter);
        connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.connect(this, observable);
        new EmptyViewBuilder().configureForSearch(listViewController.getEmptyView());
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApiPlaylist playlist = adapter.getItem(position);
        PlaylistDetailActivity.start(getActivity(), new PublicApiPlaylist(playlist).getUrn(), Screen.SEARCH_PLAYLIST_DISCO);
        eventBus.publish(EventQueue.SEARCH, SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO));
    }
}
