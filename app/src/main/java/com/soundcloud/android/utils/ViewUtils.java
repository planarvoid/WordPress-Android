package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.Iterator;
import java.util.Stack;

public class ViewUtils {

    public static void stylePtrProgress(Context context, View headerView) {
        SmoothProgressBar spb = (SmoothProgressBar) headerView.findViewById(R.id.ptr_progress);
        spb.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(context)
                .interpolator(new DecelerateInterpolator())
                .sectionsCount(3)
                .separatorLength(0)
                .width(context.getResources().getDimensionPixelSize(R.dimen.ptr_thickness))
                .speed(1f)
                .reversed(true)
                .mirrorMode(true)
                .colors(context.getResources().getIntArray(R.array.ptr_colors))
                .build());
    }

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
            private Stack<View> views = new Stack<View>();

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

                        if (child instanceof ViewGroup) pushViews((ViewGroup)child);

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
}
