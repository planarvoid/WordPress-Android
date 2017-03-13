package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;

public class CustomFontCheckBox extends AppCompatCheckBox {

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
