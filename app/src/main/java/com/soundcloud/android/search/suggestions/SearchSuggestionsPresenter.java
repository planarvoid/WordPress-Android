package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
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

public class SearchSuggestionsPresenter extends RecyclerViewPresenter<SuggestionsResult, SuggestionItem> {

    public interface SuggestionListener {
        void onScrollChanged();
        void onSearchClicked(String searchQuery);
        void onTrackClicked(Urn trackUrn);
        void onUserClicked(Urn userUrn);
    }

    private Func1<SuggestionsResult, List<SuggestionItem>> toPresentationModels(final String query) {
        return new Func1<SuggestionsResult, List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> call(SuggestionsResult searchResult) {
                final List<SuggestionItem> itemList = new ArrayList<>(searchResult.getItems().size() + 1);
                itemList.add(SuggestionItem.forSearch(query));
                for (PropertySet propertySet : searchResult.getItems()) {
                    Urn urn = propertySet.get(SearchSuggestionProperty.URN);
                    if (urn.isTrack()) {
                        itemList.add(SuggestionItem.forTrack(propertySet, query));
                    } else if (urn.isUser()) {
                        itemList.add(SuggestionItem.forUser(propertySet, query));
                    }
                }
                return itemList;
            }
        };
    }

    private final SuggestionsAdapter adapter;
    private final SearchSuggestionOperations operations;

    private SuggestionListener suggestionListener;

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
        if (fragment instanceof SuggestionListener) {
            this.suggestionListener = (SuggestionListener) fragment;
        }
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

    @Override
    protected void onItemClicked(View view, int position) {
        super.onItemClicked(view, position);
        final SuggestionItem item = adapter.getItem(position);
        if (suggestionListener != null) {
            switch (item.getKind()) {
                case SearchItem:
                    suggestionListener.onSearchClicked(item.getQuery());
                    break;
                case TrackItem:
                    suggestionListener.onTrackClicked(item.getUrn());
                    break;
                case UserItem:
                    suggestionListener.onUserClicked(item.getUrn());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled clicked item kind " + item.getKind());
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
}
