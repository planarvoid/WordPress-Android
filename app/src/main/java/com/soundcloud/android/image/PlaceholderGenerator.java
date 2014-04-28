package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import javax.inject.Inject;

public class PlaceholderGenerator {

    private static final int[][] COLOR_COMBINATIONS = new int[][]{
            {R.color.placeholder_image_purple, R.color.placeholder_image_blue},
            {R.color.placeholder_image_purple, R.color.placeholder_image_orange},
            {R.color.placeholder_image_purple, R.color.placeholder_image_beige},
            {R.color.placeholder_image_blue, R.color.placeholder_image_purple},
            {R.color.placeholder_image_blue, R.color.placeholder_image_orange},
            {R.color.placeholder_image_blue, R.color.placeholder_image_beige},
            {R.color.placeholder_image_orange, R.color.placeholder_image_purple},
            {R.color.placeholder_image_orange, R.color.placeholder_image_blue},
            {R.color.placeholder_image_orange, R.color.placeholder_image_beige},
            {R.color.placeholder_image_beige, R.color.placeholder_image_purple},
            {R.color.placeholder_image_beige, R.color.placeholder_image_blue},
            {R.color.placeholder_image_beige, R.color.placeholder_image_orange}
    };

    private final Resources resources;

    @Inject
    PlaceholderGenerator(Resources resources) {
        this.resources = resources;
    }

    public Drawable generate(String key) {
        return ImageUtils.createTransitionDrawable(resources.getDrawable(R.drawable.placeholder), build(key));
    }

    private GradientDrawable build(String key) {
        int[] colorIds = COLOR_COMBINATIONS[pickCombination(key)];
        int[] colors = {resources.getColor(colorIds[0]), resources.getColor(colorIds[1])};
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    @VisibleForTesting
    protected int pickCombination(String key) {
        return (key.hashCode() & Integer.MAX_VALUE) % COLOR_COMBINATIONS.length;
    }

}
