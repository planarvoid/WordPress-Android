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
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
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
import java.util.ArrayList;
import java.util.List;

@SuppressLint("ValidFragment")
public class SearchResultsFragment extends LightCycleSupportFragment
        implements ReactiveListComponent<ConnectableObservable<List<ListItem>>> {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";
    static final String EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT = "publishSearchSubmissionEvent";

    private static final Func1<SearchResult, List<ListItem>> TO_PRESENTATION_MODELS = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<PropertySet> sourceSets = searchResult.getItems();
            final List<ListItem> items = new ArrayList<>(sourceSets.size());
            for (PropertySet source : sourceSets) {
                final Urn urn = source.get(EntityProperty.URN);
                if (urn.isTrack()) {
                    items.add(TrackItem.from(source));
                } else if (urn.isPlaylist()) {
                    items.add(PlaylistItem.from(source));
                } else if (urn.isUser()) {
                    items.add(UserItem.from(source));
                }
            }
            return items;
        }
    };

    private static final Predicate<Urn> TRACK_PREDICATE = new Predicate<Urn>() {
        @Override
        public boolean apply(Urn input) {
            return input.isTrack();
        }
    };

    private final Function<ListItem, Urn> toUrn = new Function<ListItem, Urn>() {
        @Override
        public Urn apply(ListItem model) {
            return model.getEntityUrn();
        }
    };

    @Inject SearchOperations searchOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject EventBus eventBus;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;
    @Inject Navigator navigator;
    @Inject @LightCycle ListViewController listViewController;
    @Inject @LightCycle SearchResultsAdapter adapter;

    private int searchType;
    private boolean publishSearchSubmissionEvent;
    private ConnectableObservable<List<ListItem>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private Subscription entityChangedSubscription = Subscriptions.empty();
    private SearchOperations.SearchResultPager pager;

    private final Action1<List<ListItem>> publishOnFirstPage = new Action1<List<ListItem>>() {
        @Override
        public void call(List<ListItem> listItems) {
            if (publishSearchSubmissionEvent) {
                publishSearchSubmissionEvent = false;
                eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(getTrackingScreen(), pager.getSearchQuerySourceInfo()));
            }
        }
    };

    public static SearchResultsFragment newInstance(int type, String query, boolean publishSearchSubmissionEvent) {
        SearchResultsFragment fragment = new SearchResultsFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TYPE, type);
        bundle.putString(EXTRA_QUERY, query);
        bundle.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, publishSearchSubmissionEvent);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchResultsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    SearchResultsFragment(SearchOperations operations,
                          PlaybackOperations playbackOperations,
                          ListViewController listViewController,
                          SearchResultsAdapter adapter,
                          Provider<ExpandPlayerSubscriber> subscriberProvider,
                          EventBus eventBus,
                          SearchOperations.SearchResultPager pager,
                          Navigator navigator) {
        this.searchOperations = operations;
        this.playbackOperations = playbackOperations;
        this.listViewController = listViewController;
        this.adapter = adapter;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
        this.pager = pager;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        publishSearchSubmissionEvent = getArguments().getBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, false);
        searchType = getArguments().getInt(EXTRA_TYPE);
        pager = searchOperations.pager(searchType);
        listViewController.setAdapter(adapter, pager, TO_PRESENTATION_MODELS);
        entityChangedSubscription = eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter));
        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<List<ListItem>> buildObservable() {
        final String query = getArguments().getString(EXTRA_QUERY);
        final Observable<SearchResult> observable = searchOperations.searchResult(query, searchType);
        return pager.page(observable)
                .map(TO_PRESENTATION_MODELS)
                .doOnNext(publishOnFirstPage)
                .observeOn(mainThread())
                .replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<ListItem>> observable) {
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
        entityChangedSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ListItem item = adapter.getItem(position);
        Urn urn = item.getEntityUrn();
        SearchQuerySourceInfo searchQuerySourceInfo = pager.getSearchQuerySourceInfo(position, urn);

        if (urn.isTrack()) {
            final List<Urn> trackUrns = filterTracks(toUrn(adapter.getItems()));
            final PlaySessionSource playSessionSource = new PlaySessionSource(getTrackingScreen());
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapTrackOnScreen(getTrackingScreen(), searchQuerySourceInfo));
            playbackOperations
                    .playTracks(trackUrns, urn, trackUrns.indexOf(urn), playSessionSource)
                    .subscribe(subscriberProvider.get());
        } else if (urn.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(getTrackingScreen(), searchQuerySourceInfo));
            PlaylistDetailActivity.start(getActivity(), urn, getTrackingScreen(), false, searchQuerySourceInfo);
        } else if (urn.isUser()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapUserOnScreen(getTrackingScreen(), searchQuerySourceInfo));
            navigator.openProfile(getActivity(), urn, searchQuerySourceInfo);
        }
    }

    private List<Urn> filterTracks(List<Urn> urns) {
        return Lists.newArrayList(Iterables.filter(urns, TRACK_PREDICATE));
    }

    private List<Urn> toUrn(List<ListItem> items) {
        return Lists.transform(items, toUrn);
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
