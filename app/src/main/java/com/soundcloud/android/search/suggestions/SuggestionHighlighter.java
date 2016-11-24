package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import java.util.Locale;

final class SuggestionHighlighter {
    private SuggestionHighlighter() {
    }

    static SuggestionHighlight findHighlight(String query, String displayedText) {
        final Locale locale = Locale.getDefault();
        final int startIndex = displayedText.toLowerCase(locale).indexOf(query.toLowerCase(locale));
        final int stopIndex = startIndex + query.length();
        return new SuggestionHighlight(startIndex, stopIndex);
    }

    static void setHighlightSpans(Context context, SpannableString spanned, SuggestionHighlight suggestionHighlight) {
        spanned.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context,
                                                                       R.color.search_suggestion_unhighlighted_text)),
                        0, spanned.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        int start = suggestionHighlight.getStart();
        int end = suggestionHighlight.getEnd();
        if (start >= 0 && start < end && end > 0 && end <= spanned.length()) {
            spanned.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context,
                                                                           R.color.search_suggestion_text)),
                            start, end,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
}
