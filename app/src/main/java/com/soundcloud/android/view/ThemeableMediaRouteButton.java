package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.MediaRouteButton;
import android.util.AttributeSet;

public class ThemeableMediaRouteButton extends MediaRouteButton {

    public ThemeableMediaRouteButton(Context context) {
        super(context);
    }

    public ThemeableMediaRouteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemeableMediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLightTheme() {
        setTheme(R.drawable.mr_button_light);
    }

    public void setDarkTheme() {
        setTheme(R.drawable.mr_button_dark);
    }

    private void setTheme(@DrawableRes int themedDrawable) {
        setRemoteIndicatorDrawable(ContextCompat.getDrawable(getContext(), themedDrawable));
    }
}
