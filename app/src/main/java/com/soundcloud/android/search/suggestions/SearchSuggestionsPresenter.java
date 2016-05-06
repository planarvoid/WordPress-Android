package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.annotation.NonNull;
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

    private Func1<SuggestionsResult, List<SuggestionItem>> toPresentationModels(final String searchQuery) {
        return new Func1<SuggestionsResult, List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> call(SuggestionsResult suggestionsResult) {
                final List<SuggestionItem> itemList = new ArrayList<>(localSuggestionResult.size() + remoteSuggestionResult.size() + 1);
                itemList.add(SuggestionItem.forSearch(searchQuery));
                addSuggestionItemsToList(localSuggestionResult, itemList, searchQuery);
                addSuggestionItemsToList(remoteSuggestionResult, itemList, searchQuery);
                return itemList;
            }

            private void addSuggestionItemsToList(SuggestionsResult searchResult, List<SuggestionItem> itemList, String searchQuery) {
                for (PropertySet propertySet : searchResult.getItems()) {
                    final Urn urn = propertySet.get(SearchSuggestionProperty.URN);
                    if (urn.isTrack()) {
                        itemList.add(SuggestionItem.forTrack(propertySet, searchQuery));
                    } else if (urn.isUser()) {
                        itemList.add(SuggestionItem.forUser(propertySet, searchQuery));
                    }
                }
            }
        };
    }

    private Action1<SuggestionsResult> cacheSuggestionsResults = new Action1<SuggestionsResult>() {
        @Override
        public void call(SuggestionsResult suggestionsResult) {
            if (suggestionsResult.isLocal()) {
                localSuggestionResult = suggestionsResult;
            } else {
                remoteSuggestionResult = suggestionsResult;
            }
        }
    };

    private final SuggestionsAdapter adapter;
    private final SearchSuggestionOperations operations;

    private CollectionBinding<SuggestionsResult, SuggestionItem> collectionBinding;
    private SuggestionsResult localSuggestionResult = SuggestionsResult.emptyLocal();
    private SuggestionsResult remoteSuggestionResult = SuggestionsResult.emptyRemote();
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
                .from(buildCollectionBinding(query), toPresentationModels(query))
                .withAdapter(adapter)
                .build();
    }

    private Observable<SuggestionsResult> buildCollectionBinding(String query) {
        return operations.suggestionsFor(query).doOnNext(cacheSuggestionsResults);
    }

    void showSuggestionsFor(String query) {
        if (collectionBinding != null) {
            collectionBinding.disconnect();
        }
        collectionBinding = createCollection(query);
        retryWith(collectionBinding);
    }

    void setSuggestionListener(@NonNull SuggestionListener suggestionlistener) {
        this.suggestionListener = suggestionlistener;
    }

    @Override
    protected void onItemClicked(View view, int position) {
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

    @VisibleForTesting
    CollectionBinding<SuggestionsResult, SuggestionItem> getCollectionBinding() {
        return collectionBinding;
    }
}
