package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class SearchResultsFragment extends LightCycleFragment
        implements ReactiveListComponent<ConnectableObservable<List<PropertySet>>> {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";

    private static final Predicate<Urn> TRACK_PREDICATE = new Predicate<Urn>() {
        @Override
        public boolean apply(Urn input) {
            return input.isTrack();
        }
    };

    private final Function<PropertySet, Urn> toUrn = new Function<PropertySet, Urn>() {
        @Override
        public Urn apply(PropertySet input) {
            return getUrn(input);
        }
    };

    @Inject SearchOperations searchOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ListViewController listViewController;
    @Inject EventBus eventBus;
    @Inject SearchResultsAdapter adapter;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private int searchType;
    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private SearchOperations.SearchResultPager pager;

    private final Func1<SearchResult, List<PropertySet>> TO_PROPERTY_SET = new Func1<SearchResult, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(SearchResult searchResult) {
            return searchResult.getItems();
        }
    };

    public static SearchResultsFragment newInstance(int type, String query) {
        SearchResultsFragment fragment = new SearchResultsFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TYPE, type);
        bundle.putString(EXTRA_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchResultsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    SearchResultsFragment(SearchOperations operations,
                          PlaybackOperations playbackOperations,
                          ListViewController listViewController,
                          SearchResultsAdapter adapter,
                          Provider<ExpandPlayerSubscriber> subscriberProvider,
                          EventBus eventBus) {
        this.searchOperations = operations;
        this.playbackOperations = playbackOperations;
        this.listViewController = listViewController;
        this.adapter = adapter;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
    }

    private void addLifeCycleComponents() {
        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchType = getArguments().getInt(EXTRA_TYPE);
        pager = searchOperations.pager(searchType);
        listViewController.setAdapter(adapter, pager);

        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        final String query = getArguments().getString(EXTRA_QUERY);
        final Observable<SearchResult> observable = searchOperations.searchResult(query, searchType);
        return pager.page(observable).map(TO_PROPERTY_SET)
                .observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
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

        listViewController.connect(this, observable);
        new EmptyViewBuilder().configureForSearch(listViewController.getEmptyView());
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Urn urn = getUrn(adapter.getItem(position));
        if (urn.isTrack()) {
            final List<Urn> trackUrns = filterTracks(toUrn(adapter.getItems()));
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapTrackOnScreen(getTrackingScreen()));
            playbackOperations
                    .playTracks(trackUrns, urn, trackUrns.indexOf(urn), new PlaySessionSource(getTrackingScreen()))
                    .subscribe(subscriberProvider.get());
        } else if (urn.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(getTrackingScreen()));
            PlaylistDetailActivity.start(getActivity(), urn, getTrackingScreen());
        } else if (urn.isUser()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapUserOnScreen(getTrackingScreen()));
            startActivity(ProfileActivity.getIntent(getActivity(), urn));
        }
    }

    private List<Urn> filterTracks(List<Urn> urns) {
        return Lists.newArrayList(Iterables.filter(urns, TRACK_PREDICATE));
    }

    private List<Urn> toUrn(List<PropertySet> properties) {
        return Lists.transform(properties, toUrn);
    }

    private Urn getUrn(PropertySet propertySet) {
        return propertySet.getOrElse(UserProperty.URN, PlayableProperty.URN);
    }

    private Screen getTrackingScreen() {
        switch (searchType) {
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
