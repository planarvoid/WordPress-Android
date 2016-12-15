package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.RedrawLayoutListener;
import com.soundcloud.android.view.DefaultAnimationListener;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlayerStripView extends LinearLayout {

    private final TextView castDeviceName;
    private final int expandedHeight;
    private final int collapsedHeight;

    public PlayerStripView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.player_strip, this, true);

        castDeviceName = (TextView) findViewById(R.id.cast_device);
        collapsedHeight = getResources().getDimensionPixelSize(R.dimen.collapsed_player_strip);
        expandedHeight = getResources().getDimensionPixelSize(R.dimen.expanded_player_strip);
    }

    public int getCollapsedHeight() {
        return collapsedHeight;
    }

    public int getExpandedHeight() {
        return expandedHeight;
    }

    public boolean isCollapsed() {
        return getLayoutParams().height == collapsedHeight;
    }

    public boolean isExpanded() {
        return getLayoutParams().height == expandedHeight;
    }

    public void setCollapsed() {
        getLayoutParams().height = getCollapsedHeight();
        requestLayout();
    }

    public void setExpanded() {
        getLayoutParams().height = getExpandedHeight();
        requestLayout();
    }

    public void setHeight(int newHeight) {
        getLayoutParams().height = newHeight;
        requestLayout();
    }

    public void updateCastDeviceName(String deviceName, float alpha) {
        castDeviceName.setText(deviceName);
        castDeviceName.setAlpha(alpha);
    }

    public void updateCastDeviceName(String deviceName) {
        updateCastDeviceName(deviceName, 1.0F);
    }

    public ValueAnimator createCollapseAnimator() {
        return createValueAnimator(expandedHeight, collapsedHeight);
    }

    public ValueAnimator createExpandAnimator(DefaultAnimationListener animationListener) {
        final ValueAnimator animator = createValueAnimator(collapsedHeight, expandedHeight);
        animator.addListener(animationListener);
        return animator;
    }

    private ValueAnimator createValueAnimator(int fromValue, int toValue) {
        final ValueAnimator animator = ObjectAnimator.ofInt(fromValue, toValue);
        animator.addUpdateListener(new RedrawLayoutListener(this));
        return animator;
    }

}
