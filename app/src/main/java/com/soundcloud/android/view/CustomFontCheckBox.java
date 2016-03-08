package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class CustomFontCheckBox extends CheckBox {

    public CustomFontCheckBox(Context context) {
        super(context);
    }

    public CustomFontCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context, this, attrs);
    }

    public CustomFontCheckBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context, this, attrs);
    }
}
