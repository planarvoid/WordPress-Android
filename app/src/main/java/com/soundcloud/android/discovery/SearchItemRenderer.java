package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import java.util.List;

class SearchItemRenderer implements CellRenderer<SearchItem>, SearchController.SearchCallback {

    private final SearchController searchController;

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

    @Override
    public void performTextSearch(String query) {
//        addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
    }

    @Override
    public void performTagSearch(String tag) {
//        addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
    }

    @Override
    public void exitSearchMode() {
//        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
