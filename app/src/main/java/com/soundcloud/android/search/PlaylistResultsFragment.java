package com.soundcloud.android.search;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.PlaylistSummaryCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.EmptyViewBuilder;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class PlaylistResultsFragment extends Fragment implements EmptyViewAware,
        AdapterView.OnItemClickListener {

    public static final String TAG = "playlist_results";
    static final String KEY_PLAYLIST_TAG = "playlist_tag";

    @Inject
    SearchOperations searchOperations;
    @Inject
    ImageOperations imageOperations;
    @Inject
    PlaylistResultsAdapter adapter;
    @Inject
    ScModelManager modelManager;
    @Inject
    EventBus eventBus;

    private EmptyListView emptyListView;
    private int emptyViewStatus = EmptyListView.Status.WAITING;

    private ConnectableObservable<Page<PlaylistSummaryCollection>> playlistsObservable;
    private Subscription playlistsSubscription = Subscriptions.empty();
    private Subscription listViewSubscription;

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
    }

    @VisibleForTesting
    PlaylistResultsFragment(SearchOperations searchOperations, ImageOperations imageOperations,
                            PlaylistResultsAdapter adapter, ScModelManager modelManager, EventBus eventBus) {
        this.searchOperations = searchOperations;
        this.imageOperations = imageOperations;
        this.adapter = adapter;
        this.modelManager = modelManager;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_PLAYLIST_DISCO.get());
        playlistsObservable = preparePlaylistsObservable();
        playlistsSubscription = triggerPlaylistsObservable();
    }

    private ConnectableObservable<Page<PlaylistSummaryCollection>> preparePlaylistsObservable() {
        String playlistTag = getArguments().getString(KEY_PLAYLIST_TAG);
        ConnectableObservable<Page<PlaylistSummaryCollection>> observable = searchOperations.getPlaylistResults(playlistTag)
                .observeOn(mainThread())
                .replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        new EmptyViewBuilder().configureForSearch(emptyListView);
        emptyListView.setStatus(emptyViewStatus);
        emptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                playlistsObservable = preparePlaylistsObservable();
                playlistsSubscription = triggerPlaylistsObservable();
                listViewSubscription = playlistsObservable.subscribe(
                        new ListFragmentSubscriber<Page<PlaylistSummaryCollection>>(PlaylistResultsFragment.this));
            }
        });

        GridView gridView = (GridView) view.findViewById(android.R.id.list);
        gridView.setEmptyView(emptyListView);
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(adapter);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(imageOperations.createScrollPauseListener(false, true, adapter)));

        listViewSubscription = playlistsObservable.subscribe(
                new ListFragmentSubscriber<Page<PlaylistSummaryCollection>>(this));
    }

    @Override
    public void onDestroyView() {
        listViewSubscription.unsubscribe();
        GridView gridView = (GridView) getView().findViewById(android.R.id.list);
        gridView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        playlistsSubscription.unsubscribe();
        super.onDestroy();
    }

    private Subscription triggerPlaylistsObservable() {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        return playlistsObservable.connect();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        emptyViewStatus = status;
        if (emptyListView != null) {
            emptyListView.setStatus(status);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlaylistSummary playlist = adapter.getItem(position);
        PlaylistDetailActivity.start(getActivity(), new Playlist(playlist), modelManager, Screen.SEARCH_PLAYLIST_DISCO);
        eventBus.publish(EventQueue.SEARCH, SearchEvent.tapPlaylistOnScreen(Screen.SEARCH_PLAYLIST_DISCO));
    }
}
