package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchOperations.TYPE_PLAYLISTS;
import static com.soundcloud.android.search.SearchOperations.TYPE_TRACKS;
import static com.soundcloud.android.search.SearchOperations.TYPE_USERS;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.LegacyUpdatePlayingTrackSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsPresenter extends RecyclerViewPresenter<ListItem> {

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

    private final SearchOperations searchOperations;
    private final SearchResultsAdapter adapter;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventBus eventBus;

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
    private CompositeSubscription viewLifeCycle;

    @Inject
    SearchResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher, SearchOperations searchOperations,
                           SearchResultsAdapter adapter, MixedItemClickListener.Factory clickListenerFactory,
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
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new LegacyUpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
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
        pagingFunction = searchOperations.pagingFunction(searchType);
        return CollectionBinding
                .from(searchOperations.searchResult(searchQuery, searchType).doOnNext(publishOnFirstPage), TO_PRESENTATION_MODELS)
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

    private void trackSearchItemClick(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        if (urn.isTrack()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapTrackOnScreen(getTrackingScreen(), searchQuerySourceInfo));
        } else if (urn.isPlaylist()) {
            eventBus.publish(EventQueue.TRACKING, SearchEvent.tapPlaylistOnScreen(getTrackingScreen(), searchQuerySourceInfo));
        } else if (urn.isUser()) {
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
