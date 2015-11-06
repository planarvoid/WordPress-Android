package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class CustomFontButton extends Button {

    public CustomFontButton(Context context) {
        super(context);
    }

    public CustomFontButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, this, attrs);
    }

    public CustomFontButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, this, attrs);
    }
}
