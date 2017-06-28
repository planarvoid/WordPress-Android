package com.soundcloud.android.main;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

public class PlayerOverlayBackgroundBehavior extends CoordinatorLayout.Behavior<View> {

    private final int collapsedPlayerHeight;

    public PlayerOverlayBackgroundBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlayerOverlayBackgroundBehavior);
        collapsedPlayerHeight = typedArray.getDimensionPixelSize(R.styleable.PlayerOverlayBackgroundBehavior_player_collapsed_height, 0);

        typedArray.recycle();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency.getId() == R.id.player_root;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View playerOverlay, View player) {
        final int maximumPlayerOffset = parent.getMeasuredHeight() - collapsedPlayerHeight;
        final float currentPlayerOffset = player.getY();
        final float normalizedCurrentScroll = currentPlayerOffset / maximumPlayerOffset;

        final float alpha = 1 - normalizedCurrentScroll;

        playerOverlay.setAlpha(alpha);

        return false;
    }
}
