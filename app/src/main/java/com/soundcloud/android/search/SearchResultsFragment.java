package com.soundcloud.android.search;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class SearchResultsFragment extends Fragment
        implements ReactiveListComponent<ConnectableObservable<Page<SearchResultsCollection>>> {

    public static final String TAG = "search_results";

    static final int TYPE_ALL = 0;
    static final int TYPE_TRACKS = 1;
    static final int TYPE_PLAYLISTS = 2;
    static final int TYPE_USERS = 3;

    private static final String KEY_QUERY = "query";
    private static final String KEY_TYPE = "type";

    @Inject SearchOperations searchOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ListViewController listViewController;
    @Inject EventBus eventBus;
    @Inject SearchResultsAdapter adapter;

    private int searchType;
    private ConnectableObservable<Page<SearchResultsCollection>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private Subscription playEventSubscription = Subscriptions.empty();

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchType = getArguments().getInt(KEY_TYPE);

        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<Page<SearchResultsCollection>> buildObservable() {
        final String query = getArguments().getString(KEY_QUERY);
        Observable<Page<SearchResultsCollection>> observable;
        switch (searchType) {
            case TYPE_ALL:
                observable = searchOperations.getAllSearchResults(query);
                break;
            case TYPE_TRACKS:
                observable = searchOperations.getTrackSearchResults(query);
                break;
            case TYPE_PLAYLISTS:
                observable = searchOperations.getPlaylistSearchResults(query);
                break;
            case TYPE_USERS:
                observable = searchOperations.getUserSearchResults(query);
                break;
            default:
                throw new IllegalArgumentException("Query type not valid");
        }
        return observable.observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Page<SearchResultsCollection>> observable) {
        this.observable = observable;
        observable.subscribe(adapter);
        connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.onViewCreated(this, observable, view, adapter, adapter);
        new EmptyViewBuilder().configureForSearch(listViewController.getEmptyView());
        adapter.onViewCreated();
    }

    @Override
    public void onDestroyView() {
        listViewController.onDestroyView();
        adapter.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        playEventSubscription.unsubscribe();
        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ScResource item = adapter.getItem(position);
        Context context = getActivity();
        if (item instanceof Track) {
            eventBus.publish(EventQueue.SEARCH, SearchEvent.tapTrackOnScreen(getTrackingScreen()));
            playbackOperations.playFromAdapter(context, adapter.getItems(), position, null, getTrackingScreen());
        } else if (item instanceof Playlist) {
            eventBus.publish(EventQueue.SEARCH, SearchEvent.tapPlaylistOnScreen(getTrackingScreen()));
            playbackOperations.playFromAdapter(context, adapter.getItems(), position, null, getTrackingScreen());
        } else if (item instanceof User) {
            eventBus.publish(EventQueue.SEARCH, SearchEvent.tapUserOnScreen(getTrackingScreen()));
            context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, item));
        }
    }

    private Screen getTrackingScreen() {
        switch(searchType) {
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

}
