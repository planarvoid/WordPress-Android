package com.soundcloud.android.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.Stack;

public final class ViewUtils {

    private static final int DEFAULT_TOUCH_DELEGATE_DP = 8;

    public static int dpToPx(Context context, int dp) {
        return dpToPx(context.getResources(), dp);
    }

    public static int dpToPx(Resources resources, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }

    public static int blendColors(int first, int second, float ratio) {
        final float inverseRatio = 1f - ratio;
        float a = (Color.alpha(second) * ratio) + (Color.alpha(first) * inverseRatio);
        float r = (Color.red(second) * ratio) + (Color.red(first) * inverseRatio);
        float g = (Color.green(second) * ratio) + (Color.green(first) * inverseRatio);
        float b = (Color.blue(second) * ratio) + (Color.blue(first) * inverseRatio);
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    public static void setTouchClickable(View view, View.OnClickListener listener) {
        view.setClickable(true);
        view.setOnClickListener(listener);
        extendTouchArea(view, 10);
    }

    public static void unsetTouchClickable(View view) {
        clearTouchDelegate(view);
        view.setClickable(false);
    }

    public static void extendTouchArea(final View delegate) {
        extendTouchArea(delegate, DEFAULT_TOUCH_DELEGATE_DP);
    }

    public static void extendTouchArea(final View delegate, int extendDp) {
        final int extendPx = dpToPx(delegate.getContext(), extendDp);
        final View parent = (View) delegate.getParent();
        parent.post(() -> {
            final Rect r = new Rect();
            delegate.getHitRect(r);
            r.top -= extendPx;
            r.left -= extendPx;
            r.right += extendPx;
            r.bottom += extendPx;
            parent.setTouchDelegate(new TouchDelegate(r, delegate));
        });
    }

    public static void clearTouchDelegate(final View delegate) {
        final View parent = (View) delegate.getParent();
        parent.setTouchDelegate(null);
    }

    public static float calculateViewablePercentage(View view) {
        if (view != null) {
            final Rect onScreen = new Rect();
            final int area = view.getWidth() * view.getHeight();

            view.getGlobalVisibleRect(onScreen);

            if (area > 0) {
                final int viewableArea = onScreen.width() * onScreen.height();
                return ((float) viewableArea) / ((float) area) * 100;
            }
        }
        return 0.0f;
    }

    public static Iterable<View> childViewsOf(final ViewGroup viewGroup) {
        return new Iterable<View>() {
            private final int count = viewGroup.getChildCount();

            @Override
            public Iterator<View> iterator() {
                return new Iterator<View>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < count;
                    }

                    @Override
                    public View next() {
                        return viewGroup.getChildAt(i++);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static Iterable<View> allChildViewsOf(final ViewGroup viewGroup) {
        return new Iterable<View>() {
            private Stack<View> views = new Stack<>();

            @Override
            public Iterator<View> iterator() {
                return new Iterator<View>() {
                    {
                        pushViews(viewGroup);
                    }

                    private void pushViews(ViewGroup viewGroup) {
                        for (View view : childViewsOf(viewGroup)) {
                            views.push(view);
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return !views.empty();
                    }

                    @Override
                    public View next() {
                        View child = views.pop();

                        if (child instanceof ViewGroup) {
                            pushViews((ViewGroup) child);
                        }

                        return child;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static FragmentActivity getFragmentActivity(View anchorView) {
        return getFragmentActivity(anchorView.getContext());
    }

    public static FragmentActivity getFragmentActivity(Context context) {
        final FragmentActivity activityContext;

        if (context instanceof FragmentActivity) {
            activityContext = (FragmentActivity) context;
        } else if (context instanceof ContextWrapper) {
            final ContextWrapper contextWrapper = (ContextWrapper) context;
            activityContext = (FragmentActivity) contextWrapper.getBaseContext();
        } else {
            throw new IllegalStateException("Could not get FragmentActivity from view Context");
        }
        return activityContext;
    }

    public static void setGone(Iterable<View> views) {
        for (View v : views) {
            v.clearAnimation();
            v.setVisibility(View.GONE);
        }
    }

    public static void setVisible(Iterable<View> views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(1f);
        }
    }

    private ViewUtils() {
    }
}
