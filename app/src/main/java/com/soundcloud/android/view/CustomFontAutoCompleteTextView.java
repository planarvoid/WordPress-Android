package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class CustomFontAutoCompleteTextView extends AutoCompleteTextView {

    public CustomFontAutoCompleteTextView(Context context) {
        super(context);
    }

    public CustomFontAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, this, attrs);
    }

    public CustomFontAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, this, attrs);
    }
}
