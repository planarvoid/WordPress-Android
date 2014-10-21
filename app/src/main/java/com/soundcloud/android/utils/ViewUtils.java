package com.soundcloud.android.utils;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.Stack;

public final class ViewUtils {

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
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

    private ViewUtils() {}
}
