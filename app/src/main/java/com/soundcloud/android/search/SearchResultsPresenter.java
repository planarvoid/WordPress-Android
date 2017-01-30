package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_API_QUERY;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY_POSITION;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY_URN;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_USER_QUERY;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.FollowEntityListSubscriber;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdatePlaylistListSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.android.view.adapters.UpdateUserListSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class SearchResultsPresenter extends RecyclerViewPresenter<SearchResult, ListItem>
        implements SearchPremiumContentRenderer.OnPremiumContentClickListener {

    private static final int PREMIUM_ITEMS_POSITION = 0;
    private static final int PREMIUM_ITEMS_DISPLAYED = 1;

    private final Func1<SearchResult, List<ListItem>> toPresentationModels = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<SearchableItem> searchableItems = searchResult.getItems();
            final Optional<SearchResult> premiumContent = searchResult.getPremiumContent();
            final List<ListItem> searchItems = new ArrayList<>(searchableItems.size() + PREMIUM_ITEMS_DISPLAYED);
            if (premiumContent.isPresent()) {
                final SearchResult premiumSearchResult = premiumContent.get();
                final List<SearchableItem> premiumSearchResultItems = premiumSearchResult.getItems();
                premiumItems = buildPremiumItemsList(premiumSearchResultItems);
                searchItems.add(new SearchPremiumItem(premiumSearchResultItems,
                                                      premiumSearchResult.nextHref,
                                                      premiumSearchResult.getResultsCount()));
            }
            searchItems.addAll(searchableItems);
            return searchItems;
        }
    };

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final SearchTracker searchTracker;
    private final ScreenProvider screenProvider;

    private final Action1<SearchResult> trackSearch =
            new Action1<SearchResult>() {
                @Override
                public void call(SearchResult searchResult) {
                    queryUrn = searchResult.queryUrn.isPresent() ? searchResult.queryUrn.get() : Urn.NOT_SET;
                    boolean shouldSendTrackingState = searchTracker.shouldSendResultsScreenEvent(searchType);
                    searchTracker.setTrackingData(searchType, queryUrn, searchResult.getPremiumContent().isPresent());

                    if (publishSearchSubmissionEvent) {
                        searchTracker.trackSearchFormulationEnd(screenProvider.getLastScreen(), userQuery, autocompleteUrn(), autocompletePosition());
                    }

                    if (publishSearchSubmissionEvent || shouldSendTrackingState) {
                        publishSearchSubmissionEvent = false;

                        //We need to send the event as soon as the fragment is loaded
                        searchTracker.trackResultsScreenEvent(searchType, apiQuery);
                    }
                }
            };

    private SearchType searchType;
    private String apiQuery;
    private String userQuery;
    private Optional<Urn> autocompleteUrn = Optional.absent();
    private Optional<Integer> autocompletePosition = Optional.absent();
    private Urn queryUrn = Urn.NOT_SET;
    private Boolean publishSearchSubmissionEvent;
    private SearchOperations.SearchPagingFunction pagingFunction;
    private Optional<List<ListItem>> premiumItems = Optional.absent();
    private CompositeSubscription fragmentLifeCycle;
    private Object queryPosition;

    @Inject
    SearchResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher, SearchOperations searchOperations,
                           SearchResultsAdapter adapter, MixedItemClickListener.Factory clickListenerFactory,
                           EventBus eventBus, Navigator navigator,
                           SearchTracker searchTracker,
                           ScreenProvider screenProvider) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.searchTracker = searchTracker;
        this.screenProvider = screenProvider;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        fragmentLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(EventQueue.TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.PLAYLIST_CHANGED, new UpdatePlaylistListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.USER_CHANGED, new UpdateUserListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.FOLLOWING_CHANGED, new FollowEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        new EmptyViewBuilder().configureForSearch(getEmptyView());
    }

    @Override
    public void onDestroy(Fragment fragment) {
        fragmentLifeCycle.unsubscribe();
        searchTracker.reset();
        super.onDestroy(fragment);
    }

    @Override
    protected CollectionBinding<SearchResult, ListItem> onBuildBinding(Bundle bundle) {
        searchType = Optional.fromNullable((SearchType) bundle.getSerializable(EXTRA_TYPE)).or(SearchType.ALL);
        apiQuery = bundle.getString(EXTRA_API_QUERY);
        userQuery = bundle.getString(EXTRA_USER_QUERY);
        autocompleteUrn = Optional.fromNullable(bundle.<Urn>getParcelable(EXTRA_QUERY_URN));
        autocompletePosition = Optional.fromNullable(bundle.getInt(EXTRA_QUERY_POSITION));
        publishSearchSubmissionEvent = bundle.getBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT);
        return createCollectionBinding();
    }

    @Override
    protected CollectionBinding<SearchResult, ListItem> onRefreshBinding() {
        return createCollectionBinding();
    }

    private CollectionBinding<SearchResult, ListItem> createCollectionBinding() {
        adapter.setPremiumContentListener(this);
        pagingFunction = searchOperations.pagingFunction(searchType);
        return CollectionBinding
                .from(searchOperations
                              .searchResult(apiQuery, autocompleteUrn(), searchType)
                              .doOnNext(trackSearch), toPresentationModels)
                .withAdapter(adapter)
                .withPager(pagingFunction)
                .build();
    }

    private Optional<Urn> autocompleteUrn() {
        return searchType == SearchType.ALL ? autocompleteUrn : Optional.<Urn>absent();
    }

    private Optional<Integer> autocompletePosition() {
        return searchType == SearchType.ALL ? autocompletePosition : Optional.<Integer>absent();
    }

    private List<ListItem> buildPlaylistWithPremiumContent(List<ListItem> premiumItems) {
        // http://style/interaction/play-queue-experience/ stands that our play queues should be created with the
        // content sitting on the screen, for instance the play queue is created with the first premium item + plus
        // the rest of the normal content sitting on the adapter.
        final int numberOfItemsInPlayQueue = adapter.getItems().size();
        final List<ListItem> playables = new ArrayList<>(numberOfItemsInPlayQueue);
        if (!premiumItems.isEmpty()) {
            playables.add(premiumItems.get(0));
        }
        playables.addAll(adapter.getResultItems());
        return playables;
    }

    private Optional<List<ListItem>> buildPremiumItemsList(List<SearchableItem> premiumSearchResultItems) {
        List<ListItem> listItems = new ArrayList<>(premiumSearchResultItems.size());
        for (SearchableItem source : premiumSearchResultItems) {
            listItems.add(source);
        }
        return Optional.of(listItems);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final List<ListItem> playQueue = premiumItems.isPresent() ?
                                         buildPlaylistWithPremiumContent(this.premiumItems.get()) : adapter.getItems();
        final Urn urn = adapter.getItem(position).getUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo = pagingFunction.getSearchQuerySourceInfo(position,
                                                                                                    urn,
                                                                                                    apiQuery);
        searchTracker.trackSearchItemClick(searchType, urn, searchQuerySourceInfo);
        clickListenerFactory.create(searchType.getScreen(),
                                    searchQuerySourceInfo).onItemClick(playQueue, view.getContext(), position);
    }

    @Override
    public void onPremiumItemClicked(View view, List<ListItem> premiumItemsList) {
        final Urn firstPremiumItemUrn = premiumItemsList.get(0).getUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo =
                pagingFunction.getSearchQuerySourceInfo(PREMIUM_ITEMS_POSITION, firstPremiumItemUrn, apiQuery);
        searchTracker.trackSearchItemClick(searchType, firstPremiumItemUrn, searchQuerySourceInfo);
        clickListenerFactory.create(searchType.getScreen(), searchQuerySourceInfo)
                            .onItemClick(buildPlaylistWithPremiumContent(premiumItemsList),
                                         view.getContext(),
                                         PREMIUM_ITEMS_POSITION);
    }

    @Override
    public void onPremiumContentHelpClicked(Context context) {
        searchTracker.trackResultsUpsellClick(searchType);
        navigator.openUpgrade(context);
    }

    @Override
    public void onPremiumContentViewAllClicked(Context context, List<SearchableItem> premiumItemsSource,
                                               Optional<Link> nextHref) {
        searchTracker.trackPremiumResultsScreenEvent(queryUrn, apiQuery);
        navigator.openSearchPremiumContentResults(context, apiQuery, searchType, premiumItemsSource,
                                                  nextHref, queryUrn);
    }
}
