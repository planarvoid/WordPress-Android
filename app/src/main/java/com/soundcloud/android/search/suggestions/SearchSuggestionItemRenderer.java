package com.soundcloud.android.search.suggestions;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class SearchSuggestionItemRenderer implements CellRenderer<SuggestionItem> {

    @Bind(R.id.title) TextView titleText;

    @Inject
    SearchSuggestionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion_header, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestionItem> items) {
        ButterKnife.bind(this, itemView);
        titleText.setText(String.format(itemView.getResources().getString(R.string.search_for_query),
                                        items.get(position).userQuery()));
    }
}
