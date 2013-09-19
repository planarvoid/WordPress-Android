package com.soundcloud.android.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.view.ParallaxImageView;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import javax.annotation.Nullable;
import java.util.HashMap;

public class AbsListViewParallaxer implements AbsListView.OnScrollListener {

    @VisibleForTesting
    static final String VIEW_FOREGROUND_TAG = "foreground";

    private AbsListView.OnScrollListener mOnScrollListenerDelegate;
    private final int mParallaxStepAmount = -10;

    private final HashMap<ViewGroup, Iterable<View>> mParallaxViewMap = new HashMap<ViewGroup, Iterable<View>>();
    private final HashMap<ViewGroup, Iterable<ParallaxImageView>> mParallaxBgImageViewMap = new HashMap<ViewGroup, Iterable<ParallaxImageView>>();

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
        final float parallaxStepScaled = listView.getResources().getDisplayMetrics().density * mParallaxStepAmount;

        if (halfHeight > 0) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                applyParallaxToItemView(halfHeight, parallaxStepScaled, listView.getChildAt(i));
            }
        }
    }

    private void applyParallaxToItemView(int halfHeight, float parallaxStepScaled, View itemView) {

        if (itemView instanceof ViewGroup) {
            populateItemToParallaxViewsMaps((ViewGroup) itemView);

            for (View view : mParallaxViewMap.get(itemView)) {
                view.setTranslationY((int) (getParallaxRatio(halfHeight, itemView, view) * parallaxStepScaled));
            }

            for (ParallaxImageView view : mParallaxBgImageViewMap.get(itemView)) {
                view.setParallaxOffset(getParallaxRatio(halfHeight, itemView, view));
            }
        }
    }

    private double getParallaxRatio(int halfHeight, View itemView, View view) {
        return (itemView.getTop() + ((view.getTop() + view.getBottom()) / 2) - halfHeight) / ((double) halfHeight);
    }

    private void populateItemToParallaxViewsMaps(ViewGroup itemView) {
        if (!mParallaxViewMap.containsKey(itemView)) {
            mParallaxViewMap.put(itemView, Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
                @Override
                public boolean apply(View input) {
                    return (VIEW_FOREGROUND_TAG.equals(input.getTag()));
                }
            }));
        }

        if (!mParallaxBgImageViewMap.containsKey(itemView)) {
            mParallaxBgImageViewMap.put(itemView, Iterables.transform(Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
                @Override
                public boolean apply(View input) {
                    return input instanceof ParallaxImageView;
                }
            }), new Function<View, ParallaxImageView>() {
                @Nullable
                @Override
                public ParallaxImageView apply(@Nullable View input) {
                    return (ParallaxImageView) input;
                }
            }));
        }
    }
}
