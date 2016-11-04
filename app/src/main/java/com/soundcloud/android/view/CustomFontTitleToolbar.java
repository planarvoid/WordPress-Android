package com.soundcloud.android.view;

import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

public class CustomFontTitleToolbar extends Toolbar {

    private TextView titleText;

    public CustomFontTitleToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomFontTitleToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        titleText = (TextView) LayoutInflater.from(context).inflate(R.layout.toolbar_title, this, false);
        addView(titleText);
        applyCustomFont(context, titleText, attrs);
    }

    @Override
    public void setTitle(CharSequence title) {
        titleText.setText(title);
    }

    public void setTitleAlpha(float alpha) {
        titleText.setAlpha(alpha);
    }
}
