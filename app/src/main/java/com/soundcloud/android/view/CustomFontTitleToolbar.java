package com.soundcloud.android.view;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import com.soundcloud.android.R;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

public class CustomFontTitleToolbar extends Toolbar {

    private TextView titleText;
    private boolean darkMode;

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

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTitleToolbar);
        if (array.getBoolean(R.styleable.CustomFontTitleToolbar_darkMode, false)) {
            setDarkMode();
        } else {
            setLightMode();
        }
        array.recycle();

    }

    public void setLightMode() {
        Drawable navIcon = getNavigationIcon();
        if (navIcon != null){
            if (darkMode) {
                darkMode = false;
                if (SDK_INT < LOLLIPOP || hasNoColorFilter(navIcon)) {
                    navIcon.setColorFilter(getDarkColor(), PorterDuff.Mode.SRC_IN);
                } else {
                    animateNavIcon(navIcon, getLightColor(), getDarkColor());
                }
            }
        }

    }

    public void setDarkMode() {
        Drawable navIcon = getNavigationIcon();
        if (navIcon != null){
            if (!darkMode) {
                darkMode = true;
                if (SDK_INT < LOLLIPOP || hasNoColorFilter(navIcon)) {
                    navIcon.setColorFilter(getLightColor(), PorterDuff.Mode.SRC_IN);
                } else {
                    animateNavIcon(navIcon, getDarkColor(), getLightColor());
                }
            }
        }

    }

    @TargetApi(LOLLIPOP)
    private static boolean hasNoColorFilter(Drawable drawable) {
        return drawable.getColorFilter() == null;
    }

    @TargetApi(LOLLIPOP)
    void animateNavIcon(Drawable navIcon, int from, int to) {
        ValueAnimator anim = ValueAnimator.ofArgb(from, to);
        anim.addUpdateListener(animation -> navIcon.setColorFilter((Integer) anim.getAnimatedValue(),
                                                                   PorterDuff.Mode.SRC_IN));
        anim.start();
    }

    int getLightColor() {
        return Color.WHITE;
    }

    int getDarkColor() {
        return getResources().getColor(R.color.raven);
    }

    @Override
    public void setTitle(CharSequence title) {
        titleText.setText(title);
    }

    public void setTitleAlpha(float alpha) {
        titleText.setAlpha(alpha);
    }
}
