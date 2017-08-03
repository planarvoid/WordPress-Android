package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.findHighlight;
import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.setHighlightSpans;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.search.suggestions.SuggestionItem.AutocompletionItem;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class AutocompletionItemRenderer implements CellRenderer<AutocompletionItem> {

    private Optional<ArrowClickListener> arrowClickListener = Optional.absent();

    @Inject
    AutocompletionItemRenderer() {}

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.autocompletion_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<AutocompletionItem> items) {
        TextView titleText = ButterKnife.findById(itemView, R.id.search_title);
        titleText.setText(highlight(items.get(position), itemView.getContext()));
        setupArrow(ButterKnife.findById(itemView, R.id.arrow_icon), items.get(position), position);
    }

    void setArrowClickListener(ArrowClickListener arrowClickListener) {
        this.arrowClickListener = Optional.of(arrowClickListener);
    }

    private SpannableString highlight(AutocompletionItem autocompletion, Context context) {
        final SuggestionHighlight textHighlight = findHighlight(autocompletion.userQuery(), autocompletion.output());
        final SpannableString spanned = new SpannableString(autocompletion.output());
        setHighlightSpans(context, spanned, textHighlight);
        return spanned;
    }

    private void setupArrow(final ImageView arrowButton, final AutocompletionItem autocompletionItem, final int queryPosition) {
        arrowButton.setOnClickListener(__ -> handleArrowClick(autocompletionItem, queryPosition));
        ViewUtils.extendTouchArea(arrowButton, R.dimen.search_suggestion_arrow_touch_expansion);
    }

    private void handleArrowClick(final AutocompletionItem autocompletionItem, final int queryPosition) {
        arrowClickListener.ifPresent(listener -> listener.handleClick(autocompletionItem.userQuery(), autocompletionItem.apiQuery(), autocompletionItem.queryUrn(), queryPosition));
    }

    interface ArrowClickListener {
        void handleClick(String userQuery, String selectedSearchTerm, Optional<Urn> queryUrn, int position);
    }
}
