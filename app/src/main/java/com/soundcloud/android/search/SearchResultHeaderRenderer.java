package com.soundcloud.android.search;

import butterknife.ButterKnife;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class SearchResultHeaderRenderer implements CellRenderer<SearchResultHeaderRenderer.SearchResultHeader> {

    private final CondensedNumberFormatter condensedNumberFormatter;

    @Inject
    SearchResultHeaderRenderer(CondensedNumberFormatter condensedNumberFormatter) {
        this.condensedNumberFormatter = condensedNumberFormatter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchResultHeader> items) {
        TextView textView = ButterKnife.findById(itemView, R.id.header);
        final boolean isFirst = position == 0;
        setupWhenUpsellVisible(itemView, isFirst);
        final SearchResultHeader header = items.get(position);
        textView.setText(itemView.getResources().getString(header.typeResource(), condensedNumberFormatter.format(header.resultCount())));
    }

    private void setupWhenUpsellVisible(View itemView, boolean isVisible) {
        final View topMargin = ButterKnife.findById(itemView, R.id.top_margin);
        final View topLine = ButterKnife.findById(itemView, R.id.top_line);
        topMargin.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        topLine.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @AutoValue
    abstract static class SearchResultHeader implements ListItem {
        abstract int typeResource();

        abstract int resultCount();

        static SearchResultHeader create(SearchType searchType, SearchOperations.ContentType contentType, int resultsCount) {
            return new AutoValue_SearchResultHeaderRenderer_SearchResultHeader(Urn.NOT_SET, Optional.absent(), typeToResource(searchType, contentType), resultsCount);
        }

        private static int typeToResource(SearchType searchType, SearchOperations.ContentType contentType) {
            switch (searchType) {
                case TRACKS:
                    if (contentType == SearchOperations.ContentType.PREMIUM) {
                        return R.string.search_found_go_tracks_results_header;
                    } else {
                        return R.string.search_found_tracks_results_header;
                    }
                case USERS:
                    return R.string.search_found_people_results_header;
                case PLAYLISTS:
                    return R.string.search_found_playlists_results_header;
                case ALBUMS:
                    return R.string.search_found_albums_results_header;
                default:
                    throw new IllegalArgumentException("Unexpected search type");
            }
        }
    }

}
