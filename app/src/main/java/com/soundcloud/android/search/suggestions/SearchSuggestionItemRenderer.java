package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class SearchSuggestionItemRenderer implements CellRenderer<SearchSuggestionItem> {

    interface OnSearchClickListener {
        void onSearchClicked(String searchQuery);
    }

    private OnSearchClickListener onSearchClickListener;

    @Inject
    SearchSuggestionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return null;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchSuggestionItem> items) {

    }

    void setOnSearchClickListener(OnSearchClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onSearchClickListener = listener;
    }
}
