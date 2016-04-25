package com.soundcloud.android.search.suggestions;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.strings.Strings;

import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class SuggestionItemRenderer implements CellRenderer<SuggestionItem> {

    @Bind(R.id.icon) ImageView icon;
    @Bind(R.id.title) TextView titleText;
    @Bind(R.id.iv_search_type) ImageView searchType;

    protected final ImageOperations imageOperations;
    protected Pattern highlightPattern;

    protected SuggestionItemRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    protected void bindView(View itemView,
                            SearchSuggestionItem suggestionItem,
                            int iconRes) {
        ButterKnife.bind(this, itemView);
        this.highlightPattern =
                Pattern.compile("(^|[\\s.\\(\\)\\[\\]_-])(" +
                Pattern.quote(suggestionItem.getQuery()) + ")",
                Pattern.CASE_INSENSITIVE);
        this.titleText.setText(highlight(suggestionItem.getDisplayedText(), null));
        this.searchType.setImageResource(iconRes);
        loadIcon(itemView, suggestionItem);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_suggestion, viewGroup, false);
    }

    protected abstract void loadIcon(View itemView, ImageResource imageResource);

    //TODO: Need refactor within next PR. Do not review.
    private Spanned highlight(String displayText, String highlightData) {
        if (Strings.isBlank(highlightData)) {
            return highlightLocal(displayText);
        } else {
            return highlightRemote(displayText, highlightData);
        }
    }

    //TODO: Need refactor within next PR. Do not review.
    private Spanned highlightRemote(final String displayText, final String highlightData) {
        SpannableString spanned = new SpannableString(displayText);
        if (!TextUtils.isEmpty(highlightData)) {
            String[] regions = highlightData.split(";");
            for (String regionData : regions) {
                String[] bounds = regionData.split(",");
                setHighlightSpans(spanned, Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]));
            }
        }
        return spanned;
    }

    //TODO: Need refactor within next PR. Do not review.
    private Spanned highlightLocal(String displayText) {
        SpannableString spanned = new SpannableString(displayText);
        Matcher m = highlightPattern.matcher(displayText);
        if (m.find()) {
            setHighlightSpans(spanned, m.start(2), m.end(2));
        } else {
            setHighlightSpans(spanned, -1, -1);
        }
        return spanned;
    }

    //TODO: Need refactor within next PR. Do not review.
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
