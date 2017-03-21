package com.soundcloud.android.view;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.soundcloud.android.view.CustomFontLoader.applyCustomFont;

import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
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
        if (darkMode) {
            darkMode = false;

            getNavigationDrawable().ifPresent(this::setLightMode);
            getCastButton().ifPresent(ThemeableMediaRouteButton::setLightTheme);
        }
    }

    private void setLightMode(Drawable drawable) {
        if (SDK_INT < LOLLIPOP || hasNoColorFilter(drawable)) {
            drawable.setColorFilter(getDarkColor(), PorterDuff.Mode.SRC_IN);
        } else {
            animateToolbarIcon(drawable, getLightColor(), getDarkColor());
        }
    }

    public void setDarkMode() {
        if (!darkMode) {
            darkMode = true;

            getNavigationDrawable().ifPresent(this::setDarkMode);
            getCastButton().ifPresent(ThemeableMediaRouteButton::setDarkTheme);
        }
    }

    private void setDarkMode(Drawable drawable) {
        if (SDK_INT < LOLLIPOP || hasNoColorFilter(drawable)) {
            drawable.setColorFilter(getLightColor(), PorterDuff.Mode.SRC_IN);
        } else {
            animateToolbarIcon(drawable, getDarkColor(), getLightColor());
        }
    }

    private Optional<Drawable> getNavigationDrawable() {
        return Optional.fromNullable(getNavigationIcon());
    }

    private Optional<ThemeableMediaRouteButton> getCastButton() {
        @IdRes final int castMenuItemId = R.id.media_route_menu_item;
        if (getMenu() != null && getMenu().findItem(castMenuItemId) != null && getMenu().findItem(castMenuItemId).getActionView() instanceof ThemeableMediaRouteButton) {
            return Optional.fromNullable((ThemeableMediaRouteButton) getMenu().findItem(castMenuItemId).getActionView());
        } else {
            return Optional.absent();
        }
    }

    @TargetApi(LOLLIPOP)
    private static boolean hasNoColorFilter(Drawable drawable) {
        return drawable.getColorFilter() == null;
    }

    @TargetApi(LOLLIPOP)
    private void animateToolbarIcon(Drawable drawable, int from, int to) {
        ValueAnimator anim = ValueAnimator.ofArgb(from, to);
        anim.addUpdateListener(animation -> drawable.setColorFilter((Integer) anim.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
        anim.start();
    }

    private int getLightColor() {
        return Color.WHITE;
    }

    @ColorInt
    private int getDarkColor() {
        return ContextCompat.getColor(getContext(), R.color.charcoal);
    }

    @Override
    public void setTitle(CharSequence title) {
        titleText.setText(title);
    }

    public void setTitleAlpha(float alpha) {
        titleText.setAlpha(alpha);
    }
}
