package com.soundcloud.android.search.suggestions;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

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
        this.titleText.setText(highlight(suggestionItem.getDisplayedText(), suggestionItem.getQuery()));
        this.searchType.setImageResource(iconRes);
        loadIcon(itemView, suggestionItem);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion, viewGroup, false);
    }

    protected abstract void loadIcon(View itemView, ImageResource imageResource);

    private Spanned highlight(String displayText, String query) {
        final Locale locale = Locale.getDefault();
        final int startIndex = displayText.toLowerCase(locale).indexOf(query.toLowerCase(locale));
        final int stopIndex = startIndex + query.length();
        final SpannableString spanned = new SpannableString(displayText);
        setHighlightSpans(spanned, startIndex, stopIndex);
        return spanned;
    }

    private void setHighlightSpans(SpannableString spanned, int start, int end) {
        spanned.setSpan(new ForegroundColorSpan(ContextCompat.getColor(titleText.getContext(), R.color.search_suggestion_unhighlighted_text)),
                0, spanned.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        if (start >= 0 && start < end && end > 0 && end <= spanned.length()) {
            spanned.setSpan(new ForegroundColorSpan(ContextCompat.getColor(titleText.getContext(), R.color.search_suggestion_text)),
                    start, end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
}
