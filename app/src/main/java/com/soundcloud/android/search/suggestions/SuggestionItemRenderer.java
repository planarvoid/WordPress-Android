package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.findHighlight;
import static com.soundcloud.android.search.suggestions.SuggestionHighlighter.setHighlightSpans;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;

import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

abstract class SuggestionItemRenderer implements CellRenderer<SuggestionItem> {

    @Bind(R.id.icon) ImageView icon;
    @Bind(R.id.title) TextView titleText;
    @Bind(R.id.iv_search_type) ImageView searchType;

    protected final ImageOperations imageOperations;

    protected SuggestionItemRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    protected void bindView(View itemView,
                            SearchSuggestionItem suggestionItem,
                            int iconRes) {
        ButterKnife.bind(this, itemView);
        this.titleText.setText(highlight(suggestionItem));
        this.searchType.setImageResource(iconRes);
        loadIcon(itemView, suggestionItem);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion, viewGroup, false);
    }

    protected abstract void loadIcon(View itemView, ImageResource imageResource);

    private Spanned highlight(SearchSuggestionItem suggestionItem) {
        final SuggestionHighlight suggestionHighlight =
                suggestionItem.getSuggestionHighlight().or(findHighlight(
                        suggestionItem.userQuery(), suggestionItem.getDisplayedText()));
        final SpannableString spanned = new SpannableString(suggestionItem.getDisplayedText());
        setHighlightSpans(titleText.getContext(), spanned, suggestionHighlight);
        return spanned;
    }
}
