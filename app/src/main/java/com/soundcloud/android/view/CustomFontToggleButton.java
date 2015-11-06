package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public class CustomFontToggleButton extends ToggleButton {

    public CustomFontToggleButton(Context context) {
        super(context);
    }

    public CustomFontToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, this, attrs);
    }

    public CustomFontToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, this, attrs);
    }
}
