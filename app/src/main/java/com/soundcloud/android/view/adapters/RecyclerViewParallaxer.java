package com.soundcloud.android.view.adapters;

import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.ParallaxImageView;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

public class RecyclerViewParallaxer extends RecyclerView.OnScrollListener {

    private static final int PARALLAX_STEP_AMOUNT = -10;

    @VisibleForTesting
    static final String VIEW_FOREGROUND_TAG = "foreground";

    private final Map<ViewGroup, Iterable<View>> parallaxViewMap = new HashMap<>();
    private final Map<ViewGroup, Iterable<ParallaxImageView>> parallaxBgImageViewMap = new HashMap<>();

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        final int halfHeight = recyclerView.getHeight() / 2;
        final float parallaxStepScaled = recyclerView.getResources().getDisplayMetrics().density * PARALLAX_STEP_AMOUNT;

        if (halfHeight > 0) {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                applyParallaxToItemView(halfHeight, parallaxStepScaled, recyclerView.getChildAt(i));
            }
        }
    }

    private void applyParallaxToItemView(int halfHeight, float parallaxStepScaled, View itemView) {
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

    private double getParallaxRatio(int halfHeight, View itemView, View view) {
        return (itemView.getTop() + ((view.getTop() + view.getBottom()) / 2) - halfHeight) / ((double) halfHeight);
    }

    private void populateItemToParallaxViewsMaps(ViewGroup itemView) {
        if (!parallaxViewMap.containsKey(itemView)) {
            parallaxViewMap.put(itemView, Iterables.filter(ViewUtils.allChildViewsOf(itemView), input -> (VIEW_FOREGROUND_TAG.equals(input.getTag()))));
        }

        if (!parallaxBgImageViewMap.containsKey(itemView)) {
            parallaxBgImageViewMap.put(itemView,
                                       Iterables.transform(Iterables.filter(ViewUtils.allChildViewsOf(itemView),
                                                                            input -> input instanceof ParallaxImageView),
                                                           input -> (ParallaxImageView) input));
        }
    }
}
