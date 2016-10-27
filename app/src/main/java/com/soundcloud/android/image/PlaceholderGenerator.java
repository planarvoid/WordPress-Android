package com.soundcloud.android.image;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.graphics.Palette;

import javax.inject.Inject;

public class PlaceholderGenerator {

    private static final int[][] COLOR_COMBINATIONS = new int[][]{
            {R.color.ak_placeholder_aubergine, R.color.ak_placeholder_slate},
            {R.color.ak_placeholder_aubergine, R.color.ak_placeholder_salmon},
            {R.color.ak_placeholder_aubergine, R.color.ak_placeholder_greige},
            {R.color.ak_placeholder_slate, R.color.ak_placeholder_aubergine},
            {R.color.ak_placeholder_slate, R.color.ak_placeholder_salmon},
            {R.color.ak_placeholder_slate, R.color.ak_placeholder_greige},
            {R.color.ak_placeholder_salmon, R.color.ak_placeholder_aubergine},
            {R.color.ak_placeholder_salmon, R.color.ak_placeholder_slate},
            {R.color.ak_placeholder_salmon, R.color.ak_placeholder_greige},
            {R.color.ak_placeholder_greige, R.color.ak_placeholder_aubergine},
            {R.color.ak_placeholder_greige, R.color.ak_placeholder_slate},
            {R.color.ak_placeholder_greige, R.color.ak_placeholder_salmon}
    };

    protected final Resources resources;

    @Inject
    PlaceholderGenerator(Resources resources) {
        this.resources = resources;
    }

    public TransitionDrawable generateTransitionDrawable(String key) {
        return ImageUtils.createTransitionDrawable(getLoadingDrawable(), generateDrawable(key));
    }

    protected Drawable getLoadingDrawable() {
        return resources.getDrawable(R.color.gray_background);
    }

    public GradientDrawable generateDrawable(String key) {
        int[] colorIds = COLOR_COMBINATIONS[pickCombination(key)];
        int[] colors = {resources.getColor(colorIds[0]), resources.getColor(colorIds[1])};
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    public GradientDrawable generateDrawableFromPalette(String key, Palette palette) {
        int[] colorIds = COLOR_COMBINATIONS[pickCombination(key)];
        final int darkMutedColor = palette.getDarkMutedColor(resources.getColor(colorIds[0]));
        final int lightMutedColor = palette.getLightMutedColor(resources.getColor(colorIds[1]));
        int[] colors = {darkMutedColor, lightMutedColor};
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    @VisibleForTesting
    protected int pickCombination(String key) {
        return (key.hashCode() & Integer.MAX_VALUE) % COLOR_COMBINATIONS.length;
    }

}
