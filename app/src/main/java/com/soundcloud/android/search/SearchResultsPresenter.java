package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.events.EventQueue;
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
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
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

class SearchResultsPresenter extends RecyclerViewPresenter<ListItem>
        implements SearchPremiumContentRenderer.OnPremiumContentClickListener {

    private final Func1<SearchResult, List<ListItem>> toPresentationModels = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<PropertySet> sourceSetsItems = searchResult.getItems();
            final Optional<SearchResult> premiumContent = searchResult.getPremiumContent();
            final List<ListItem> searchItems = new ArrayList<>(sourceSetsItems.size() + 1);
            if (premiumContent.isPresent() && featureFlags.isEnabled(Flag.SEARCH_RESULTS_HIGH_TIER)) {
                final SearchResult premiumSearchResult = premiumContent.get();
                searchItems.add(SearchResultItem.buildPremiumItem(premiumSearchResult.getItems(),
                        premiumSearchResult.nextHref, premiumSearchResult.getResultsCount()));
            }
            for (PropertySet source : sourceSetsItems) {
                searchItems.add(SearchResultItem.fromPropertySet(source).build());
            }
            return searchItems;
        }
    };

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;
    private final FeatureFlags featureFlags;
    private final Navigator navigator;
    private final SearchTracker searchTracker;

    private final Action1<SearchResult> trackSearch = new Action1<SearchResult>() {
        @Override
        public void call(SearchResult searchResult) {
            searchTracker.setQueryUrnForSearchType(searchType, searchResult.queryUrn);
            if (publishSearchSubmissionEvent) {
                publishSearchSubmissionEvent = false;
                searchTracker.trackSearchSubmission(searchType, searchResult.queryUrn);
                searchTracker.trackResultsScreenEvent(searchType);
            }
        }
    };

    private int searchType;
    private String searchQuery;
    private Boolean publishSearchSubmissionEvent;
    private SearchOperations.SearchPagingFunction pagingFunction;
    private CompositeSubscription fragmentLifeCycle;

    @Inject
    SearchResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher, SearchOperations searchOperations,
                           SearchResultsAdapter adapter, MixedItemClickListener.Factory clickListenerFactory,
                           EventBus eventBus, FeatureFlags featureFlags, Navigator navigator,
                           SearchTracker searchTracker) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
        this.navigator = navigator;
        this.searchTracker = searchTracker;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        fragmentLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)));
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
    protected CollectionBinding<ListItem> onBuildBinding(Bundle bundle) {
        searchType = bundle.getInt(EXTRA_TYPE);
        searchQuery = bundle.getString(EXTRA_QUERY);
        publishSearchSubmissionEvent = bundle.getBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT);
        return createCollectionBinding();
    }

    @Override
    protected CollectionBinding<ListItem> onRefreshBinding() {
        return createCollectionBinding();
    }

    private CollectionBinding<ListItem> createCollectionBinding() {
        adapter.setPremiumContentListener(this);
        pagingFunction = searchOperations.pagingFunction(searchType);
        return CollectionBinding
                .from(searchOperations
                        .searchResult(searchQuery, searchType)
                        .doOnNext(trackSearch), toPresentationModels)
                .withAdapter(adapter)
                .withPager(pagingFunction)
                .build();
    }

    private List<ListItem> buildPlaylistWithPremiumContent(List<ListItem> premiumItems) {
        // http://style/interaction/play-queue-experience/ stands that our play queues should be created with the
        // content sitting on the screen, for instance the play queue is created with the first premium item + plus
        // the rest of the normal content sitting on the adapter.
        final int numberOfItemsInPlayQueue = adapter.getItems().size();
        final List<ListItem> playables = new ArrayList<>(numberOfItemsInPlayQueue);
        playables.add(premiumItems.get(0));
        playables.addAll(adapter.getItems().subList(1, numberOfItemsInPlayQueue));
        return playables;
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getEntityUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo = pagingFunction.getSearchQuerySourceInfo(position, urn);
        searchTracker.trackSearchItemClick(searchType, urn, searchQuerySourceInfo);
        clickListenerFactory.create(searchTracker.getTrackingScreen(searchType),
                searchQuerySourceInfo).onItemClick(adapter.getItems(), view, position);
    }

    @Override
    public void onPremiumItemClicked(View view, List<ListItem> premiumItems) {
        clickListenerFactory.create(searchTracker.getTrackingScreen(searchType),
                null).onItemClick(buildPlaylistWithPremiumContent(premiumItems), view, 0);
    }

    @Override
    public void onPremiumContentHelpClicked(Context context) {
        navigator.openUpgrade(context);
    }

    @Override
    public void onPremiumContentViewAllClicked(Context context, List<PropertySet> premiumItemsSource, Optional<Link> nextHref) {
        navigator.openSearchPremiumContentResults(context, searchQuery, searchType, premiumItemsSource, nextHref);
    }
}
