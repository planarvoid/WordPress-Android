package com.soundcloud.android.search;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.FOLLOWING_CHANGED;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;
import static com.soundcloud.android.events.EventQueue.USER_CHANGED;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY_URN;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
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
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class SearchPremiumResultsPresenter extends RecyclerViewPresenter<SearchResult, ListItem>
        implements SearchUpsellRenderer.OnUpsellClickListener {

    private static final Func1<SearchResult, List<ListItem>> TO_PRESENTATION_MODELS = searchResult -> {
        final List<ListItem> sourceSetsItems = searchResult.getItems();
        final List<ListItem> searchItems = new ArrayList<>(sourceSetsItems.size() + 1);
        for (ListItem source : sourceSetsItems) {
            searchItems.add(source);
        }
        return searchItems;
    };

    private final Func1<SearchResult, SearchResult> addUpsellItem = new Func1<SearchResult, SearchResult>() {
        @Override
        public SearchResult call(SearchResult searchResult) {
            if (featureOperations.upsellHighTier()) {
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
    private final FeatureOperations featureOperations;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final SearchTracker searchTracker;
    private final SearchPlayQueueFilter playQueueFilter;

    private SearchOperations.SearchPagingFunction pagingFunction;
    private CompositeSubscription viewLifeCycle;

    private SearchType searchType;
    private String searchQuery;

    @Inject
    SearchPremiumResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                  SearchOperations searchOperations,
                                  SearchResultsAdapter adapter,
                                  MixedItemClickListener.Factory clickListenerFactory,
                                  FeatureOperations featureOperations,
                                  Navigator navigator,
                                  EventBus eventBus,
                                  SearchTracker searchTracker,
                                  SearchPlayQueueFilter playQueueFilter) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.featureOperations = featureOperations;
        this.navigator = navigator;
        this.eventBus = eventBus;
        this.searchTracker = searchTracker;
        this.playQueueFilter = playQueueFilter;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(PLAYLIST_CHANGED, new UpdatePlaylistListSubscriber(adapter)),
                eventBus.subscribe(USER_CHANGED, new UpdateUserListSubscriber(adapter)),
                eventBus.subscribe(LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),
                eventBus.subscribe(FOLLOWING_CHANGED, new FollowEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onUpsellClicked(Context context) {
        searchTracker.trackPremiumResultsUpsellClick();
        navigator.openUpgrade(context);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo = pagingFunction.getSearchQuerySourceInfo(position, urn, searchQuery);
        searchTracker.trackSearchPremiumItemClick(urn, searchQuerySourceInfo);
        clickListenerFactory.create(searchTracker.getPremiumTrackingScreen(), searchQuerySourceInfo)
                            .onItemClick(playQueueFilter.correctQueue(adapter.getItems(), position),
                                         view.getContext(),
                                         playQueueFilter.correctPosition(position));
    }

    @Override
    protected CollectionBinding<SearchResult, ListItem> onBuildBinding(Bundle bundle) {
        searchQuery = bundle.getString(EXTRA_SEARCH_QUERY);
        searchType = Optional.fromNullable((SearchType) bundle.getSerializable(EXTRA_SEARCH_TYPE)).or(SearchType.ALL);
        final List<Urn> premiumContentList = bundle.getParcelableArrayList(EXTRA_PREMIUM_CONTENT_RESULTS);
        final Optional<Link> nextHref = Optional.fromNullable((Link) bundle.getParcelable(
                EXTRA_PREMIUM_CONTENT_NEXT_HREF));
        final Urn queryUrn = bundle.getParcelable(EXTRA_SEARCH_QUERY_URN);
        return createCollectionBinding(searchOperations.searchPremiumResultFrom(premiumContentList,
                                                                                nextHref,
                                                                                queryUrn));
    }

    @Override
    protected CollectionBinding<SearchResult, ListItem> onRefreshBinding() {
        return createCollectionBinding(searchOperations.searchPremiumResult(searchQuery, searchType));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding<SearchResult, ListItem> createCollectionBinding(Observable<SearchResult> searchResultObservable) {
        adapter.setUpsellListener(this);
        pagingFunction = searchOperations.pagingFunction(searchType);
        return CollectionBinding
                .from(searchResultObservable.map(addUpsellItem), TO_PRESENTATION_MODELS)
                .withAdapter(adapter)
                .withPager(pagingFunction)
                .build();
    }
}
