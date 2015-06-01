/*
 * This class comes from the Android sample library. It is copied verbatim, minus the removal
 * of the mPrefix
 * 
 * https://developer.android.com/samples/SwipeRefreshMultipleViews/src/com.example.android.swiperefreshmultipleviews/MultiSwipeRefreshLayout.html
 */
package com.soundcloud.android.view;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;


public class MultiSwipeRefreshLayout extends SwipeRefreshLayout {

    private View[] swipeableChildren;
    private boolean measured;
    private boolean preMeasureRefreshing;

    public MultiSwipeRefreshLayout(Context context) {
        super(context);
    }

    public MultiSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Hack to overcome refreshing before measurement (after orientation change this happens)
     * https://code.google.com/p/android/issues/detail?id=77712
     */

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!measured) {
            measured = true;
            setRefreshing(preMeasureRefreshing);
        }
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        if (measured) {
            super.setRefreshing(refreshing);
        } else {
            preMeasureRefreshing = refreshing;
        }
    }

    /**
     * Set the children which can trigger a refresh by swiping down when they are visible. These
     * views need to be a descendant of this view.
     */
    public void setSwipeableChildren(final int... ids) {
        assert ids != null;

        // Iterate through the ids and find the Views
        swipeableChildren = new View[ids.length];
        for (int i = 0; i < ids.length; i++) {
            swipeableChildren[i] = findViewById(ids[i]);
        }
    }

    public void setSwipeableChildren(final View... views) {
        assert views != null;

        // Iterate through the ids and find the Views
        swipeableChildren = views;
    }

    /**
     * This method controls when the swipe-to-refresh gesture is triggered. By returning false here
     * we are signifying that the view is in a state where a refresh gesture can start.
     *
     * <p>As {@link android.support.v4.widget.SwipeRefreshLayout} only supports one direct child by
     * default, we need to manually iterate through our swipeable children to see if any are in a
     * state to trigger the gesture. If so we return false to start the gesture.
     */
    @Override
    public boolean canChildScrollUp() {
        if (swipeableChildren != null && swipeableChildren.length > 0) {
            // Iterate through the scrollable children and check if any of them can not scroll up
            for (View view : swipeableChildren) {
                if (view != null && view.isShown() && !canViewScrollUp(view)) {
                    // If the view is shown, and can not scroll upwards, return false and start the
                    // gesture.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Utility method to check whether a {@link View} can scroll up from it's current position.
     * Handles platform version differences, providing backwards compatible functionality where
     * needed.
     */
    private static boolean canViewScrollUp(View view) {
        return ViewCompat.canScrollVertically(view, -1);
    }
}