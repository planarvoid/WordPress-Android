package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.findHighlight;
import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.setHighlightSpans;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.search.suggestions.SuggestionItem.AutocompletionItem;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class AutocompletionItemRenderer implements CellRenderer<AutocompletionItem> {

    @Inject
    AutocompletionItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.autocompletion_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<AutocompletionItem> items) {
        TextView titleText = ButterKnife.findById(itemView, R.id.search_title);
        titleText.setText(highlight(items.get(position), itemView.getContext()));
    }

    private SpannableString highlight(AutocompletionItem autocompletion, Context context) {
        final SuggestionHighlight textHighlight = findHighlight(autocompletion.userQuery(), autocompletion.output());
        final SpannableString spanned = new SpannableString(autocompletion.output());
        setHighlightSpans(context, spanned, textHighlight);
        return spanned;
    }

}
