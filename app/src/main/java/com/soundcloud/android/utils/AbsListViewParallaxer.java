package com.soundcloud.android.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import java.util.HashMap;

public class AbsListViewParallaxer implements AbsListView.OnScrollListener {

    @VisibleForTesting
    static final String VIEW_TOP_TAG = "parallax_top";
    static final String VIEW_MID_TAG = "parallax_mid";

    private AbsListView.OnScrollListener mOnScrollListenerDelegate;
    final int mParallaxStepAmount = 10;

    HashMap<ViewGroup, Iterable<View>> parallaxMidViewMap = new HashMap<ViewGroup, Iterable<View>>();
    HashMap<ViewGroup, Iterable<View>> parallaxTopViewMap = new HashMap<ViewGroup, Iterable<View>>();

    public AbsListViewParallaxer(AbsListView.OnScrollListener scrollListenerDelegate) {
        this.mOnScrollListenerDelegate = scrollListenerDelegate;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListenerDelegate != null) {
            mOnScrollListenerDelegate.onScrollStateChanged(view,
                    scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mOnScrollListenerDelegate != null) {
            mOnScrollListenerDelegate.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            scrollChanged(view);
        }
    }

    private void scrollChanged(AbsListView listView) {
        final int halfHeight = listView.getHeight() / 2;
        final float mParallaxStepScaled = listView.getResources().getDisplayMetrics().density * mParallaxStepAmount;

        if (halfHeight > 0) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                applyParallaxToItemView(halfHeight, mParallaxStepScaled, listView.getChildAt(i));
            }
        }
    }

    private void applyParallaxToItemView(int halfHeight, float mParallaxStepScaled, View itemView) {

        if (itemView instanceof ViewGroup) {
            populateItemToParallaxViewsMaps((ViewGroup) itemView);

            for (View view : parallaxMidViewMap.get(itemView)) {
                view.setTranslationY((int) (getParallaxRatio(halfHeight, itemView, view) * mParallaxStepScaled));
            }
            for (View view : parallaxTopViewMap.get(itemView)) {
                view.setTranslationY((int) (getParallaxRatio(halfHeight, itemView, view) * mParallaxStepScaled * 2));
            }
        }
    }

    private double getParallaxRatio(int halfHeight, View itemView, View view) {
        return (itemView.getTop() + ((view.getTop() + view.getBottom()) / 2) - halfHeight) / ((double) halfHeight);
    }

    private void populateItemToParallaxViewsMaps(ViewGroup itemView) {
        if (!parallaxMidViewMap.containsKey(itemView)) {
            parallaxMidViewMap.put(itemView, Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
                @Override
                public boolean apply(View input) {
                    return (VIEW_MID_TAG.equals(input.getTag()));
                }
            }));
        }

        if (!parallaxTopViewMap.containsKey(itemView)) {
            parallaxTopViewMap.put(itemView, Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
                @Override
                public boolean apply(View input) {
                    return (VIEW_TOP_TAG.equals(input.getTag()));
                }
            }));
        }
    }
}
