package com.soundcloud.android.view.tour;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

public class TourTextView extends TextView {
    public TourTextView(Context context) {
        super(context);
    }

    public TourTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TourTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setText(CharSequence text, BufferType type) {
        SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
        spanBuilder.append(text);

        spanBuilder.setSpan(
            new BackgroundColorSpan(0xCC000000),
            0,
            text.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        super.setText(spanBuilder, type);
    }

}
