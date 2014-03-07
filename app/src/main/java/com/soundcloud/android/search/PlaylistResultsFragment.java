package com.soundcloud.android.search;

import static rx.android.OperationPaged.Page;
import static rx.android.observables.AndroidObservable.fromFragment;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
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
    SearchOperations mSearchOperations;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    PlaylistResultsAdapter mAdapter;
    @Inject
    ScModelManager mModelManager;
    @Inject
    EventBus mEventBus;

    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    private Subscription mSubscription = Subscriptions.empty();

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
        mSearchOperations = searchOperations;
        mImageOperations = imageOperations;
        mAdapter = adapter;
        mModelManager = modelManager;
        mEventBus = eventBus;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_PLAYLIST_DISCO.get());
        loadPlaylistResults();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                loadPlaylistResults();
            }
        });

        GridView gridView = (GridView) view.findViewById(android.R.id.list);
        gridView.setEmptyView(mEmptyListView);
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mAdapter);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(mImageOperations.createScrollPauseListener(false, true, mAdapter)));
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    private void loadPlaylistResults() {
        String playlistTag = getArguments().getString(KEY_PLAYLIST_TAG);
        ConnectableObservable<Page<PlaylistSummaryCollection>> observable =
                fromFragment(this, mSearchOperations.getPlaylistResults(playlistTag)).publish();

        setEmptyViewStatus(EmptyListView.Status.WAITING);
        observable.subscribe(mAdapter);
        observable.subscribe(new ListFragmentSubscriber<Page<PlaylistSummaryCollection>>(this));
        mSubscription = observable.connect();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PlaylistSummary playlist = mAdapter.getItem(position);
        PlaylistDetailActivity.start(getActivity(), new Playlist(playlist), mModelManager, Screen.SEARCH_PLAYLIST_DISCO);
    }
}
