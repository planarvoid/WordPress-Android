package com.soundcloud.android.search;

import auto.parcel.AutoParcel;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
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
    public SearchResultHeaderRenderer(CondensedNumberFormatter condensedNumberFormatter) {
        this.condensedNumberFormatter = condensedNumberFormatter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchResultHeader> items) {
        TextView textView = ButterKnife.findById(itemView, R.id.header);
        final View topMargin = ButterKnife.findById(itemView, R.id.top_margin);
        topMargin.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        final SearchResultHeader header = items.get(position);
        final String type = itemView.getResources().getString(header.typeResource());
        textView.setText(itemView.getResources().getString(R.string.search_found_results_header, condensedNumberFormatter.format(header.resultCount()), type));
    }

    @AutoParcel
    public abstract static class SearchResultHeader implements SearchableItem {
        abstract int typeResource();

        abstract int resultCount();

        static SearchResultHeader create(SearchType searchType, SearchOperations.ContentType contentType, int resultsCount) {
            return new AutoParcel_SearchResultHeaderRenderer_SearchResultHeader(Optional.absent(), Urn.NOT_SET, typeToResource(searchType, contentType), resultsCount);
        }

        private static int typeToResource(SearchType searchType, SearchOperations.ContentType contentType) {
            switch (searchType) {
                case TRACKS:
                    if (contentType == SearchOperations.ContentType.PREMIUM) {
                        return R.string.top_results_go_tracks;
                    } else {
                        return R.string.top_results_tracks;
                    }
                case USERS:
                    return R.string.top_results_people;
                case PLAYLISTS:
                    return R.string.top_results_playlists;
                case ALBUMS:
                    return R.string.top_results_albums;
                default:
                    throw new IllegalArgumentException("Unexpected search type");
            }
        }
    }

}
