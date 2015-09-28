package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;

import javax.inject.Inject;
import java.util.List;

class SearchItemRenderer implements CellRenderer<SearchItem> {

    interface SearchListener {
        void onSearchTextPerformed(Context context, String query);

        void onLaunchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri);
    }

    private final SuggestionsAdapter adapter;

    private SearchListener searchListener;

    @Inject
    public SearchItemRenderer(SuggestionsAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem> list) {
        initSearchView((AutoCompleteTextView) itemView.findViewById(R.id.search));
    }

    private void initSearchView(final AutoCompleteTextView searchView) {
        searchView.setAdapter(adapter);
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (searchListener != null) {
                    if (adapter.isSearchItem(position)) {
                        searchListener.onSearchTextPerformed(searchView.getContext(), searchView.getText().toString().trim());
                    } else {
                        final SearchQuerySourceInfo searchQuerySourceInfo = getQuerySourceInfo(position);
                        final Uri itemUri = adapter.getItemIntentData(position);
                        searchListener.onLaunchSearchSuggestion(searchView.getContext(), adapter.getUrn(position),
                                searchQuerySourceInfo, itemUri);
                    }
                }
            }
        });
    }

    private SearchQuerySourceInfo getQuerySourceInfo(int position) {
        SearchQuerySourceInfo searchQuerySourceInfo = null;
        Urn queryUrn = adapter.getQueryUrn(position);
        if (!queryUrn.equals(Urn.NOT_SET)) {
            searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn,
                    adapter.getQueryPosition(position),
                    adapter.getUrn(position));
        }
        return searchQuerySourceInfo;
    }

    void setSearchListener(SearchListener searchListener) {
        checkNotNull(searchListener);
        this.searchListener = searchListener;
    }
}
