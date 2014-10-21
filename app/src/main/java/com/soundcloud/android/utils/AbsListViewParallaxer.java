package com.soundcloud.android.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.view.ParallaxImageView;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AbsListViewParallaxer implements AbsListView.OnScrollListener {
    private static final int PARALLAX_STEP_AMOUNT = -10;

    @VisibleForTesting
    static final String VIEW_FOREGROUND_TAG = "foreground";

    private final AbsListView.OnScrollListener onScrollListenerDelegate;

    private final Map<ViewGroup, Iterable<View>> parallaxViewMap = new HashMap<>();
    private final Map<ViewGroup, Iterable<ParallaxImageView>> parallaxBgImageViewMap = new HashMap<>();

    public AbsListViewParallaxer(@Nullable AbsListView.OnScrollListener scrollListenerDelegate) {
        this.onScrollListenerDelegate = scrollListenerDelegate;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (onScrollListenerDelegate != null) {
            onScrollListenerDelegate.onScrollStateChanged(view,
                    scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (onScrollListenerDelegate != null) {
            onScrollListenerDelegate.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            scrollChanged(view);
        }
    }

    private void scrollChanged(AbsListView listView) {
        final int halfHeight = listView.getHeight() / 2;
        final float parallaxStepScaled = listView.getResources().getDisplayMetrics().density * PARALLAX_STEP_AMOUNT;

        if (halfHeight > 0) {
            for (int i = 0; i < listView.getChildCount(); i++) {
                applyParallaxToItemView(halfHeight, parallaxStepScaled, listView.getChildAt(i));
            }
        }
    }

    @TargetApi(11)
    private void applyParallaxToItemView(int halfHeight, float parallaxStepScaled, View itemView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (itemView instanceof ViewGroup) {
                populateItemToParallaxViewsMaps((ViewGroup) itemView);

                for (View view : parallaxViewMap.get(itemView)) {
                    view.setTranslationY((int) (getParallaxRatio(halfHeight, itemView, view) * parallaxStepScaled));
                }

                for (ParallaxImageView view : parallaxBgImageViewMap.get(itemView)) {
                    view.setParallaxOffset(getParallaxRatio(halfHeight, itemView, view));
                }
            }
        }
    }

    private double getParallaxRatio(int halfHeight, View itemView, View view) {
        return (itemView.getTop() + ((view.getTop() + view.getBottom()) / 2) - halfHeight) / ((double) halfHeight);
    }

    private void populateItemToParallaxViewsMaps(ViewGroup itemView) {
        if (!parallaxViewMap.containsKey(itemView)) {
            parallaxViewMap.put(itemView, Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
                @Override
                public boolean apply(View input) {
                    return (VIEW_FOREGROUND_TAG.equals(input.getTag()));
                }
            }));
        }

        if (!parallaxBgImageViewMap.containsKey(itemView)) {
            parallaxBgImageViewMap.put(itemView, Iterables.transform(Iterables.filter(ViewUtils.allChildViewsOf(itemView), new Predicate<View>() {
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
