package com.soundcloud.android.search;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.EmptyViewBuilder;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class SearchResultsFragment extends ListFragment implements EmptyViewAware, AdapterView.OnItemClickListener {

    public static final String TAG = "search_results";

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    private static final String KEY_QUERY = "query";
    private static final String KEY_TYPE = "type";

    @Inject
    SearchOperations mSearchOperations;
    @Inject
    PlaybackOperations mPlaybackOperations;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    EventBus mEventBus;
    @Inject
    SearchResultsAdapter mAdapter;

    private int mSearchType;
    private ConnectableObservable<Page<SearchResultsCollection>> mSearchObservable;
    private Subscription mSearchSubscription = Subscriptions.empty();
    private Subscription mPlayEventSubscription = Subscriptions.empty();
    private Subscription mListViewSubscription = Subscriptions.empty();

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
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    SearchResultsFragment(SearchOperations searchOperations, PlaybackOperations playbackOperations,
                          ImageOperations imageOperations, EventBus eventBus, SearchResultsAdapter adapter) {
        mSearchOperations = searchOperations;
        mPlaybackOperations = playbackOperations;
        mImageOperations = imageOperations;
        mEventBus = eventBus;
        mAdapter = adapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSearchType = getArguments().getInt(KEY_TYPE);
        setListAdapter(mAdapter);

        mSearchObservable = prepareSearchResultsObservable();
        mSearchSubscription = loadSearchResults();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        new EmptyViewBuilder().configureForSearch(mEmptyListView);

        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                mSearchObservable = prepareSearchResultsObservable();
                mSearchSubscription = loadSearchResults();
                mListViewSubscription = mSearchObservable.subscribe(
                        new ListFragmentSubscriber<Page<SearchResultsCollection>>(SearchResultsFragment.this));
            }
        });

        getListView().setEmptyView(mEmptyListView);
        getListView().setOnItemClickListener(this);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        getListView().setOnScrollListener(mImageOperations.createScrollPauseListener(false, true, mAdapter));

        mListViewSubscription = mSearchObservable.subscribe(new ListFragmentSubscriber<Page<SearchResultsCollection>>(this));
    }

    @Override
    public void onDestroyView() {
        mListViewSubscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mSearchSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
        mPlayEventSubscription = mEventBus.subscribe(EventQueue.PLAY_CONTROL, new PlayEventSubscriber());
    }

    @Override
    public void onPause() {
        mPlayEventSubscription.unsubscribe();
        super.onPause();
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
        ScResource item = mAdapter.getItem(position);
        Context context = getActivity();
        if (item instanceof Track) {
            mEventBus.publish(EventQueue.SEARCH, SearchEvent.tapTrackOnScreen(getTrackingScreen()));
            mPlaybackOperations.playFromAdapter(context, mAdapter.getItems(), position, null, getTrackingScreen());
        } else if (item instanceof Playlist) {
            mEventBus.publish(EventQueue.SEARCH, SearchEvent.tapPlaylistOnScreen(getTrackingScreen()));
            mPlaybackOperations.playFromAdapter(context, mAdapter.getItems(), position, null, getTrackingScreen());
        } else if (item instanceof User) {
            mEventBus.publish(EventQueue.SEARCH, SearchEvent.tapUserOnScreen(getTrackingScreen()));
            context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, item));
        }
    }

    private Screen getTrackingScreen() {
        switch(mSearchType) {
            case TYPE_ALL:
                return Screen.SEARCH_EVERYTHING;
            case TYPE_TRACKS:
                return Screen.SEARCH_TRACKS;
            case TYPE_PLAYLISTS:
                return Screen.SEARCH_PLAYLISTS;
            case TYPE_USERS:
                return Screen.SEARCH_USERS;
            default:
                throw new IllegalArgumentException("Query type not valid");
        }
    }

    private ConnectableObservable<Page<SearchResultsCollection>> prepareSearchResultsObservable() {
        final String query = getArguments().getString(KEY_QUERY);
        Observable<Page<SearchResultsCollection>> observable;
        switch (mSearchType) {
            case TYPE_ALL:
                observable = mSearchOperations.getAllSearchResults(query);
                break;
            case TYPE_TRACKS:
                observable = mSearchOperations.getTrackSearchResults(query);
                break;
            case TYPE_PLAYLISTS:
                observable = mSearchOperations.getPlaylistSearchResults(query);
                break;
            case TYPE_USERS:
                observable = mSearchOperations.getUserSearchResults(query);
                break;
            default:
                throw new IllegalArgumentException("Query type not valid");
        }
        ConnectableObservable<Page<SearchResultsCollection>> connectableObservable
                = observable.observeOn(mainThread()).replay();
        connectableObservable.subscribe(mAdapter);
        return connectableObservable;
    }

    private Subscription loadSearchResults() {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        return mSearchObservable.connect();
    }

    private final class PlayEventSubscriber extends DefaultSubscriber<PlayControlEvent> {
        @Override
        public void onNext(PlayControlEvent event) {
            mAdapter.notifyDataSetChanged();
        }
    }

}
