package com.soundcloud.android.search;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
import com.soundcloud.java.strings.Strings;
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
            final List<ListItem> searchableItems = searchResult.getItems();
            final Optional<SearchResult> premiumContent = searchResult.getPremiumContent();
            final List<ListItem> searchItems = new ArrayList<>(searchableItems.size() + PREMIUM_ITEMS_DISPLAYED);
            if (premiumContent.isPresent()) {
                final SearchResult premiumSearchResult = premiumContent.get();
                final List<ListItem> premiumSearchResultItems = premiumSearchResult.getItems();
                premiumItems = buildPremiumItemsList(premiumSearchResultItems);
                searchItems.add(new SearchPremiumItem(premiumSearchResultItems,
                                                      premiumSearchResult.nextHref,
                                                      premiumSearchResult.getResultsCount()));
            }
            searchItems.addAll(searchableItems);
            return searchItems;
        }
    };

    private final Func1<SearchResult, SearchResult> addHeaderItem = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult searchResult) {
            if (featureFlags.isEnabled(Flag.SEARCH_TOP_RESULTS)) {
                final SearchResultHeaderRenderer.SearchResultHeader headerItem = SearchResultHeaderRenderer.SearchResultHeader.create(searchType, contentType, searchResult.getResultsCount());
                searchResult.addItem(0, headerItem);
            }
            return searchResult;
        }
    };

    private final Func1<SearchResult, SearchResult> addUpsellItem = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult searchResult) {
            if (contentType == SearchOperations.ContentType.PREMIUM && featureOperations.upsellHighTier()) {
                searchTracker.trackPremiumResultsUpsellImpression();
                final UpsellSearchableItem upsellItem = UpsellSearchableItem.forUpsell();
                searchResult.addItem(0, upsellItem);
            }
            return searchResult;
        }
    };

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final SearchTracker searchTracker;
    private final ScreenProvider screenProvider;
    private final SearchPlayQueueFilter playQueueFilter;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final PerformanceMetricsEngine performanceMetricsEngine;

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
    private SearchOperations.ContentType contentType;

    @Inject
    SearchResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           SearchOperations searchOperations,
                           SearchResultsAdapter adapter,
                           MixedItemClickListener.Factory clickListenerFactory,
                           EventBus eventBus, Navigator navigator,
                           SearchTracker searchTracker,
                           ScreenProvider screenProvider,
                           SearchPlayQueueFilter playQueueFilter,
                           FeatureFlags featureFlags,
                           FeatureOperations featureOperations,
                           PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.searchTracker = searchTracker;
        this.screenProvider = screenProvider;
        this.playQueueFilter = playQueueFilter;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.performanceMetricsEngine = performanceMetricsEngine;
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
        final SearchFragmentArgs args = bundle.getParcelable(SearchResultsFragment.EXTRA_ARGS);
        if (args != null) {
            searchType = args.searchType();
            apiQuery = args.apiQuery();
            userQuery = args.userQuery();
            autocompleteUrn = args.queryUrn();
            autocompletePosition = args.queryPosition();
            publishSearchSubmissionEvent = args.publishSearchSubmissionEvent();
            contentType = args.isPremium() ? SearchOperations.ContentType.PREMIUM : SearchOperations.ContentType.NORMAL;
        } else {
            searchType = SearchType.ALL;
            apiQuery = Strings.EMPTY;
            userQuery = Strings.EMPTY;
            autocompleteUrn = Optional.absent();
            autocompletePosition = Optional.absent();
            publishSearchSubmissionEvent = false;
            contentType = SearchOperations.ContentType.NORMAL;
        }
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
                              .searchResult(apiQuery, autocompleteUrn(), searchType, contentType)
                              .map(addHeaderItem)
                              .map(addUpsellItem)
                              .doOnNext(trackSearch)
                              .doOnCompleted(this::endMeasuringSearchTime),
                      toPresentationModels)
                .withAdapter(adapter)
                .withPager(pagingFunction)
                .build();
    }

    private void endMeasuringSearchTime() {
        if (searchType == SearchType.ALL) {
            MetricParams params = new MetricParams().putString(MetricKey.SCREEN, Screen.SEARCH_MAIN.toString());
            PerformanceMetric metric = PerformanceMetric.builder()
                                                        .metricType(MetricType.PERFORM_SEARCH)
                                                        .metricParams(params)
                                                        .build();
            performanceMetricsEngine.endMeasuring(metric);
        }
    }

    private Optional<Urn> autocompleteUrn() {
        return searchType == SearchType.ALL ? autocompleteUrn : Optional.absent();
    }

    private Optional<Integer> autocompletePosition() {
        return searchType == SearchType.ALL ? autocompletePosition : Optional.absent();
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

    private Optional<List<ListItem>> buildPremiumItemsList(List<ListItem> premiumSearchResultItems) {
        List<ListItem> listItems = new ArrayList<>(premiumSearchResultItems.size());
        for (ListItem source : premiumSearchResultItems) {
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
        final List<ListItem> playQueue = playQueueFilter.correctQueue((premiumItems.isPresent() ? buildPlaylistWithPremiumContent(this.premiumItems.get()) : adapter.getItems()), position);
        final Urn urn = adapter.getItem(position).getUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo = pagingFunction.getSearchQuerySourceInfo(position,
                                                                                                    urn,
                                                                                                    apiQuery);
        searchTracker.trackSearchItemClick(searchType, urn, searchQuerySourceInfo);
        clickListenerFactory.create(searchType.getScreen(), searchQuerySourceInfo).onItemClick(playQueue, view.getContext(), playQueueFilter.correctPosition(position));
    }

    @Override
    public void onPremiumItemClicked(View view, List<ListItem> premiumItemsList) {
        final Urn firstPremiumItemUrn = premiumItemsList.get(0).getUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo =
                pagingFunction.getSearchQuerySourceInfo(PREMIUM_ITEMS_POSITION, firstPremiumItemUrn, apiQuery);
        searchTracker.trackSearchItemClick(searchType, firstPremiumItemUrn, searchQuerySourceInfo);
        clickListenerFactory.create(searchType.getScreen(), searchQuerySourceInfo)
                            .onItemClick(playQueueFilter.correctQueue(buildPlaylistWithPremiumContent(premiumItemsList), PREMIUM_ITEMS_POSITION),
                                         view.getContext(),
                                         playQueueFilter.correctPosition(PREMIUM_ITEMS_POSITION));
    }

    @Override
    public void onPremiumContentHelpClicked(Context context) {
        searchTracker.trackResultsUpsellClick(searchType);
        navigator.openUpgrade(context);
    }

    @Override
    public void onPremiumContentViewAllClicked(Context context, List<Urn> premiumItemsSource, Optional<Link> nextHref) {
        searchTracker.trackPremiumResultsScreenEvent(queryUrn, apiQuery);
        navigator.openSearchPremiumContentResults(context, apiQuery, searchType, premiumItemsSource, nextHref, queryUrn);
    }
}
