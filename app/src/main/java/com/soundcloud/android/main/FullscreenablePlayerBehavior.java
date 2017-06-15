package com.soundcloud.android.main;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;
import static com.soundcloud.android.view.status.StatusBarUtils.getStatusBarHeight;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;

/**
 * Subclass of {@link BottomSheetBehavior} meant to properly size the player when hosted in a {@link FullscreenablePlayerActivity}.
 * This is needed because if the Activity is "full screen" we need to take into account the status bar height when measuring the player view.
 * If Activity hosting the target view is not a {@link FullscreenablePlayerActivity} this behaves just like a {@link BottomSheetBehavior}.
 *
 * @param <V>
 */
public class FullscreenablePlayerBehavior<V extends View> extends LockableBottomSheetBehavior<V> {

    public FullscreenablePlayerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullscreenablePlayerBehavior() {
        super();
    }

    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, V child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        final FragmentActivity activity = getFragmentActivity(child.getContext());

        if (shouldSubtractStatusBarHeight(activity)) {
            final int mode = MeasureSpec.getMode(parentHeightMeasureSpec);
            final int parentSize = MeasureSpec.getSize(parentHeightMeasureSpec);

            final int targetHeight = parentSize - getStatusBarHeight(activity);

            child.measure(parentWidthMeasureSpec, MeasureSpec.makeMeasureSpec(targetHeight, mode));

            return true;
        }

        return super.onMeasureChild(parent, child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
    }

    private boolean shouldSubtractStatusBarHeight(FragmentActivity activity) {
        return activity instanceof FullscreenablePlayerActivity && ((FullscreenablePlayerActivity) activity).shouldBeFullscreen();
    }
}
