package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.suggestions.SuggestionItem.AutocompletionItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import io.reactivex.Observable;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.NumberPicker;

import javax.inject.Inject;
import java.util.List;

public class SearchSuggestionsPresenter extends RecyclerViewPresenter<List<SuggestionItem>, SuggestionItem>
        implements AutocompletionItemRenderer.ArrowClickListener {

    public interface SuggestionListener {

        void onScrollChanged();

        void onSearchClicked(String apiQuery, String userQuery);

        void onSuggestionClicked(String suggestion);

        void onAutocompleteClicked(String query, String userQuery, String output, Optional<Urn> queryUrn, int position);

        void onAutocompleteArrowClicked(String userQuery, String selectedSearchTerm, Optional<Urn> queryUrn, Optional<Integer> queryInteger);

    }

    private final SuggestionsAdapter adapter;

    private final SearchSuggestionOperations operations;
    private final MixedItemClickListener.Factory clickListenerFactory;
    private final EventTracker eventTracker;

    private CollectionBinding<List<SuggestionItem>, SuggestionItem> collectionBinding;

    private SuggestionListener suggestionListener;
    private String searchQuery;

    @Inject
    SearchSuggestionsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               SuggestionsAdapter adapter,
                               SearchSuggestionOperations operations,
                               MixedItemClickListener.Factory clickListenerFactory,
                               EventTracker eventTracker) {
        super(swipeRefreshAttacher, Options.list().build());
        this.adapter = adapter;
        this.operations = operations;
        this.clickListenerFactory = clickListenerFactory;
        this.eventTracker = eventTracker;
        adapter.setAutocompleteArrowClickListener(this);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        super.onDestroy(fragment);
        this.suggestionListener = null;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.getRecyclerView().addOnScrollListener(new SuggestionsScrollListener());
    }

    @Override
    protected CollectionBinding<List<SuggestionItem>, SuggestionItem> onBuildBinding(Bundle fragmentArgs) {
        return createCollection(Strings.EMPTY);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void handleClick(String userQuery, String selectedSearchTerm, Optional<Urn> queryUrn, int queryInteger) {
        suggestionListener.onAutocompleteArrowClicked(userQuery, selectedSearchTerm, queryUrn, Optional.of(queryInteger));
    }

    private CollectionBinding<List<SuggestionItem>, SuggestionItem> createCollection(String query) {
        return CollectionBinding
                .fromV2(buildCollectionBinding(query))
                .withAdapter(adapter)
                .build();
    }

    private Observable<List<SuggestionItem>> buildCollectionBinding(String query) {
        return operations.suggestionsFor(query);
    }

    void showSuggestionsFor(String query) {
        if (query != null && !query.equals(this.searchQuery)) {
            this.searchQuery = query;
            if (collectionBinding != null) {
                collectionBinding.disconnect();
            }
            collectionBinding = createCollection(query);
            retryWith(collectionBinding);
        }
    }

    void setSuggestionListener(@NonNull SuggestionListener suggestionlistener) {
        this.suggestionListener = suggestionlistener;
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final SuggestionItem item = adapter.getItem(position);
        if (suggestionListener != null) {
            switch (item.kind()) {
                case SearchItem:
                    suggestionListener.onSearchClicked(item.userQuery(), item.userQuery());
                    break;
                case AutocompletionItem:
                    final AutocompletionItem autocompletionItem = (AutocompletionItem) item;
                    suggestionListener.onAutocompleteClicked(autocompletionItem.apiQuery(), autocompletionItem.userQuery(), autocompletionItem.output(), autocompletionItem.queryUrn(), position);
                    break;
                default:
                    onSuggestionClicked(position, item, view.getContext());
                    break;
            }
        }
    }

    private void onSuggestionClicked(int position, SuggestionItem item, Context context) {
        final SearchSuggestionItem suggestionItem = (SearchSuggestionItem) item;
        suggestionListener.onSuggestionClicked(suggestionItem.displayedText());
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.NOT_SET, suggestionItem.userQuery());
        final Screen searchSuggestions = Screen.SEARCH_SUGGESTIONS;
        eventTracker.trackSearch(SearchEvent.tapLocalSuggestionOnScreen(searchSuggestions, suggestionItem.getUrn(), suggestionItem.userQuery(), position));
        clickListenerFactory.create(searchSuggestions, searchQuerySourceInfo).onItemClick(suggestionItem, context);
    }

    private class SuggestionsScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && suggestionListener != null) {
                suggestionListener.onScrollChanged();
            }
        }
    }

    @VisibleForTesting
    CollectionBinding<List<SuggestionItem>, SuggestionItem> getCollectionBinding() {
        return collectionBinding;
    }
}
