package com.soundcloud.android.search.suggestions;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.strings.Strings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class SearchSuggestionItemRenderer implements CellRenderer<SearchSuggestionItem> {

    interface OnSearchClickListener {
        void onSearchClicked(String searchQuery);
    }

    @Bind(R.id.title) TextView titleText;

    private OnSearchClickListener onSearchClickListener;
    private String query = Strings.EMPTY;

    @Inject
    SearchSuggestionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion_header, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchSuggestionItem> items) {
        ButterKnife.bind(this, itemView);
        query = items.get(position).getQuery();
        titleText.setText(String.format(itemView.getResources().getString(R.string.search_for_query), query));
    }

    @OnClick(R.id.search_suggestions_container)
    public void onSearchClick() {
        if (onSearchClickListener!= null) {
            onSearchClickListener.onSearchClicked(query);
        }
    }

    void setOnSearchClickListener(OnSearchClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.onSearchClickListener = listener;
    }
}
