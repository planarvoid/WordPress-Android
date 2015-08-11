package com.soundcloud.android.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.Stack;

public final class ViewUtils {

    public static int dpToPx(Context context, int dp) {
        return dpToPx(context.getResources(), dp);
    }

    public static int dpToPx(Resources resources, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
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

    public static void extendTouchArea(final View delegate, int extendDp) {
        final int extendPx = dpToPx(delegate.getContext(), extendDp);
        final View parent = (View) delegate.getParent();
        parent.post(new Runnable() {
            public void run() {
                final Rect r = new Rect();
                delegate.getHitRect(r);
                r.top -= extendPx;
                r.left -= extendPx;
                r.right += extendPx;
                r.bottom += extendPx;
                parent.setTouchDelegate(new TouchDelegate(r, delegate));
            }
        });
    }

    public static void clearTouchDelegate(final View delegate) {
        final View parent = (View) delegate.getParent();
        parent.setTouchDelegate(null);
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

    private ViewUtils() {
    }
}
