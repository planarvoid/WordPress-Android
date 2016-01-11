package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
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
import android.widget.Toast;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsPresenter extends RecyclerViewPresenter<ListItem>
        implements SearchPremiumContentRenderer.OnPremiumContentClickListener {

    private final Func1<SearchResult, List<ListItem>> toPresentationModels = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<PropertySet> sourceSetsItems = searchResult.getItems();
            final Optional<SearchResult> premiumContent = searchResult.getPremiumContent();
            final List<ListItem> searchItems = new ArrayList<>(sourceSetsItems.size() + 1);
            if (premiumContent.isPresent() && featureFlags.isEnabled(Flag.SEARCH_RESULTS_HIGH_TIER)) {
                searchItems.add(SearchItem.buildPremiumItem(premiumContent.get().getItems()));
            }
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
    private final FeatureFlags featureFlags;

    private final Action1<SearchResult> publishOnFirstPage = new Action1<SearchResult>() {
        @Override
        public void call(SearchResult ignored) {
            if (publishSearchSubmissionEvent) {
                publishSearchSubmissionEvent = false;
                eventBus.publish(EventQueue.TRACKING, SearchEvent.searchStart(getTrackingScreen(), pagingFunction.getSearchQuerySourceInfo()));
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
                           EventBus eventBus, FeatureFlags featureFlags) {
        super(swipeRefreshAttacher, Options.list().build());
        this.searchOperations = searchOperations;
        this.adapter = adapter;
        this.clickListenerFactory = clickListenerFactory;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
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
                .from(searchOperations.searchResult(searchQuery, searchType).doOnNext(publishOnFirstPage), toPresentationModels)
                .withAdapter(adapter)
                .withPager(pagingFunction)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final Urn urn = adapter.getItem(position).getEntityUrn();
        final SearchQuerySourceInfo searchQuerySourceInfo = pagingFunction.getSearchQuerySourceInfo(position, urn);
        trackSearchItemClick(urn, searchQuerySourceInfo);
        clickListenerFactory.create(getTrackingScreen(), searchQuerySourceInfo).onItemClick(adapter.getItems(), view, position);
    }

    @Override
    public void onPremiumItemClicked(View view, List<ListItem> premiumItems) {
        //TODO: SearchQuerySourceInfo is null until search premium content tracking is implemented
        clickListenerFactory.create(getTrackingScreen(), null).onItemClick(premiumItems, view, 0);
    }

    @Override
    public void onPremiumContentViewAllClicked(Context context, List<PropertySet> premiumItemsSource) {
        //TODO: Implementation of this will be done in a second part in a new PR.
        Toast.makeText(context, "Premium items --> " + premiumItemsSource.size(), Toast.LENGTH_SHORT).show();
    }

    private void trackSearchItemClick(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchItem searchItem = SearchItem.fromUrn(urn);
        if (searchItem.isTrack()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapTrackOnScreen(getTrackingScreen(), searchQuerySourceInfo));
        } else if (searchItem.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(getTrackingScreen(), searchQuerySourceInfo));
        } else if (searchItem.isUser()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapUserOnScreen(getTrackingScreen(), searchQuerySourceInfo));
        }
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
