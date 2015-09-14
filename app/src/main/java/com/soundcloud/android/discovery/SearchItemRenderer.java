package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import java.util.List;

class SearchItemRenderer implements CellRenderer<SearchItem>, SearchController.SearchCallback {

    interface OnSearchListener {
        void onSearchTextPerformed(Context context, String query);

        void onLaunchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri);
    }

    private final SearchController searchController;

    private OnSearchListener onSearchListener;

    @Inject
    public SearchItemRenderer(SearchController searchController) {
        this.searchController = searchController;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem> list) {
        final AutoCompleteTextView searchView = (AutoCompleteTextView) itemView.findViewById(R.id.search);
        searchController.bindSearchView(searchView, this);
    }

    void setOnSearchListener(OnSearchListener onSearchListener) {
        checkNotNull(onSearchListener);
        this.onSearchListener = onSearchListener;
    }

    @Override
    public void performTextSearch(Context context, String query) {
        if (onSearchListener != null) {
            onSearchListener.onSearchTextPerformed(context, query);
        }
    }

    @Override
    public void launchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri) {
        if (onSearchListener != null) {
            onSearchListener.onLaunchSearchSuggestion(context, urn, searchQuerySourceInfo, itemUri);
        }
    }
}
