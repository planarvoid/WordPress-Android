package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.suggestions.SuggestionItem.AutocompletionItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.strings.Strings;
import rx.Observable;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.NumberPicker;

import javax.inject.Inject;
import java.util.List;

public class SearchSuggestionsPresenter extends RecyclerViewPresenter<List<SuggestionItem>, SuggestionItem> {

    public interface SuggestionListener {
        void onScrollChanged();

        void onSearchClicked(String searchQuery);

        void onSuggestionClicked(SuggestionItem item);

        void onAutocompleteClicked(String query, String output, String queryUrn);
    }

    private final SuggestionsAdapter adapter;
    private final SearchSuggestionOperations operations;

    private CollectionBinding<List<SuggestionItem>, SuggestionItem> collectionBinding;
    private SuggestionListener suggestionListener;
    private String searchQuery;

    @Inject
    SearchSuggestionsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               SuggestionsAdapter adapter,
                               SearchSuggestionOperations operations) {
        super(swipeRefreshAttacher, Options.list().build());
        this.adapter = adapter;
        this.operations = operations;
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

    private CollectionBinding<List<SuggestionItem>, SuggestionItem> createCollection(String query) {
        return CollectionBinding
                .from(buildCollectionBinding(query))
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
                    suggestionListener.onSearchClicked(item.userQuery());
                    break;
                case AutocompletionItem:
                    final AutocompletionItem autocompletionItem = (AutocompletionItem) item;
                    suggestionListener.onAutocompleteClicked(autocompletionItem.apiQuery(), autocompletionItem.output(), autocompletionItem.queryUrn());
                    break;
                default:
                    suggestionListener.onSuggestionClicked(item);
                    break;
            }
        }
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
