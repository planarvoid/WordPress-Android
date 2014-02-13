package com.soundcloud.android.search;


import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

public class SearchResultsFragment extends ListFragment implements EmptyViewAware, AdapterView.OnItemClickListener {

    public static final String TAG = "search_results";

    public static final int TYPE_ALL = 0;
    public static final int TYPE_TRACKS = 1;
    public static final int TYPE_PLAYLISTS = 2;
    public static final int TYPE_PEOPLE = 3;

    private static final String KEY_QUERY = "query";
    private static final String KEY_TYPE = "type";

    @Inject
    SearchOperations mSearchOperations;

    @Inject
    PlaybackOperations mPlaybackOperations;

    @Inject
    SearchResultsAdapter mAdapter;

    private Subscription mSubscription = Subscriptions.empty();

    private EmptyListView mEmptyListView;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    public static SearchResultsFragment newInstance(int type, String query) {
        SearchResultsFragment fragment = new SearchResultsFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, type);
        bundle.putString(KEY_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchResultsFragment() {
        setRetainInstance(true);
        new DaggerDependencyInjector().fromAppGraphWithModules(new SearchModule()).inject(this);
    }

    @VisibleForTesting
    SearchResultsFragment(SearchOperations searchOperations, PlaybackOperations playbackOperations, SearchResultsAdapter adapter) {
        mSearchOperations = searchOperations;
        mPlaybackOperations = playbackOperations;
        mAdapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(mAdapter);

        ConnectableObservable<SearchResultsCollection> searchResultsObservable = buildSearchResultsObservable();
        loadSearchResults(searchResultsObservable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_results_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                ConnectableObservable<SearchResultsCollection> searchResultsObservable = buildSearchResultsObservable();
                loadSearchResults(searchResultsObservable);
            }
        });
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
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
        int type = mAdapter.getItemViewType(position);
        Context context = getActivity();
        if (type == SearchResultsAdapter.TYPE_PLAYABLE) {
            mPlaybackOperations.playFromAdapter(context, mAdapter.getItems(), position, null, Screen.SEARCH_EVERYTHING);
        } else if (type == SearchResultsAdapter.TYPE_USER) {
            context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, mAdapter.getItem(position)));
        }
    }

    private ConnectableObservable<SearchResultsCollection> buildSearchResultsObservable() {
        final String query = getArguments().getString(KEY_QUERY);
        return AndroidObservable.fromFragment(this, mSearchOperations.getSearchResults(query)).replay();
    }

    private void loadSearchResults(ConnectableObservable<SearchResultsCollection> observable) {
        ListFragmentObserver<SearchResultsCollection> loadingStateObserver =
                new ListFragmentObserver<SearchResultsCollection>(this);

        setEmptyViewStatus(EmptyListView.Status.WAITING);
        observable.subscribe(mAdapter);
        observable.subscribe(loadingStateObserver);
        mSubscription = observable.connect();
    }
}
