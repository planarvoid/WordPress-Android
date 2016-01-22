package com.soundcloud.android.search;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class SearchPremiumResultsPresenter extends RecyclerViewPresenter<ListItem> {

    private static final Func1<SearchResult, List<ListItem>> TO_PRESENTATION_MODELS = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<PropertySet> sourceSetsItems = searchResult.getItems();
            final List<ListItem> searchItems = new ArrayList<>(sourceSetsItems.size());
            for (PropertySet source : sourceSetsItems) {
                searchItems.add(SearchItem.fromPropertySet(source).build());
            }
            return searchItems;
        }
    };

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    private int searchType;
    private String searchQuery;

    @Inject
    SearchPremiumResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                  SearchOperations searchOperations,
                                  SearchResultsAdapter adapter,
                                  MixedItemClickListener.Factory clickListenerFactory,
                                  EventBus eventBus) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
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
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        //TODO: SearchQuerySourceInfo is null until search premium content tracking is implemented
        clickListenerFactory.create(Screen.SEARCH_PREMIUM_CONTENT, null).onItemClick(adapter.getItems(), view, position);
    }

    @Override
    protected CollectionBinding<ListItem> onBuildBinding(Bundle bundle) {
        searchQuery = bundle.getString(EXTRA_SEARCH_QUERY);
        searchType = bundle.getInt(EXTRA_SEARCH_TYPE);
        final List<PropertySet> premiumContentList = bundle.getParcelableArrayList(EXTRA_PREMIUM_CONTENT_RESULTS);
        final Optional<Link> nextHref = Optional.fromNullable((Link) bundle.getParcelable(EXTRA_PREMIUM_CONTENT_NEXT_HREF));
        final SearchResult searchResult = SearchResult.fromPropertySets(premiumContentList, nextHref);
        return createCollectionBinding(Observable.just(searchResult));
    }

    @Override
    protected CollectionBinding<ListItem> onRefreshBinding() {
        return createCollectionBinding(searchOperations.searchPremiumResult(searchQuery, searchType));
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding<ListItem> createCollectionBinding(Observable<SearchResult> searchResultObservable) {
        return CollectionBinding
                .from(searchResultObservable, TO_PRESENTATION_MODELS)
                .withAdapter(adapter)
                .withPager(searchOperations.pagingPremiumFunction(searchType))
                .build();
    }
}
