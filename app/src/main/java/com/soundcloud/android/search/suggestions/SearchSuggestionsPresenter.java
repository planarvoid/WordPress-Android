package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.strings.Strings;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.NumberPicker;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionsPresenter extends RecyclerViewPresenter<SuggestionsResult, SuggestionItem>
        implements SuggestionsAdapter.SuggestionItemListener {

    public interface SearchListener {
        void onSearchClicked(String searchQuery);
    }

    private Func1<SuggestionsResult, List<SuggestionItem>> toPresentationModels(final String query) {
        return new Func1<SuggestionsResult, List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> call(SuggestionsResult searchResult) {
                final List<SuggestionItem> itemList = new ArrayList<>();
                itemList.add(new SearchSuggestionItem(query));
                return itemList;
            }
        };
    }

    private final SuggestionsAdapter adapter;
    private final SearchSuggestionOperations operations;

    private SearchListener searchListener;

    @Inject
    SearchSuggestionsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               SuggestionsAdapter adapter,
                               SearchSuggestionOperations operations) {
        super(swipeRefreshAttacher, Options.list().build());
        this.adapter = adapter;
        this.operations = operations;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.adapter.registerAdapterDataObserver(new SuggestionsVisibilityController());
        this.adapter.setSuggestionListener(this);
        if (fragment instanceof SearchListener) {
            this.searchListener = (SearchListener) fragment;
        }
    }

    @Override
    public void onDestroy(Fragment fragment) {
        super.onDestroy(fragment);
        this.searchListener = null;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        this.getRecyclerView().addOnScrollListener(new SuggestionsScrollListener());
    }

    @Override
    protected CollectionBinding<SuggestionsResult, SuggestionItem> onBuildBinding(Bundle fragmentArgs) {
        return createCollection(Strings.EMPTY);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private CollectionBinding<SuggestionsResult, SuggestionItem> createCollection(String query) {
        return CollectionBinding
                .from(operations.suggestionsFor(query), toPresentationModels(query))
                .withAdapter(adapter)
                .build();
    }

    void showSuggestionsFor(String query) {
        adapter.clear();
        retryWith(createCollection(query));
    }

    void clearSuggestions() {
        //TODO
    }

    @Override
    public void onSearchClicked(String searchQuery) {
        if (searchListener != null) {
            searchListener.onSearchClicked(searchQuery);
        }
    }

    @Override
    public void onTrackClicked(String searchQuery) {
//        deactivateSearchView();
        //TODO: start track
    }

    @Override
    public void onUserClicked(String searchQuery) {
//        deactivateSearchView();
        //TODO: view profile
    }

    private class SuggestionsVisibilityController extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            //TODO
//            if (!adapter.isEmpty()) {
//                showSearchSuggestionsView();
//            } else {
//                hideSearchSuggestionsView();
//            }
        }
    }

    private class SuggestionsScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                //TODO
//                hideKeyboard();
            }
        }
    }
}
