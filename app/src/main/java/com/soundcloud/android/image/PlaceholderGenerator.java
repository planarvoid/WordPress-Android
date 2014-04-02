package com.soundcloud.android.image;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import javax.inject.Inject;

public class PlaceholderGenerator {

    private static final int[][] COLOR_COMBINATIONS = new int[][] {
            { R.color.placeholder_image_purple , R.color.placeholder_image_blue},
            { R.color.placeholder_image_purple , R.color.placeholder_image_orange },
            { R.color.placeholder_image_purple , R.color.placeholder_image_beige },
            { R.color.placeholder_image_blue   , R.color.placeholder_image_purple },
            { R.color.placeholder_image_blue   , R.color.placeholder_image_orange },
            { R.color.placeholder_image_blue   , R.color.placeholder_image_beige },
            { R.color.placeholder_image_orange , R.color.placeholder_image_purple },
            { R.color.placeholder_image_orange , R.color.placeholder_image_blue},
            { R.color.placeholder_image_orange , R.color.placeholder_image_beige },
            { R.color.placeholder_image_beige  , R.color.placeholder_image_purple },
            { R.color.placeholder_image_beige  , R.color.placeholder_image_blue},
            { R.color.placeholder_image_beige  , R.color.placeholder_image_orange }
    };

    private final Resources mResources;

    @Inject
    PlaceholderGenerator(Resources resources) {
        mResources = resources;
    }

    public Drawable generate(String key) {
        // What is going on here? See: http://findbugs.blogspot.de/2006/09/is-mathabs-broken.html
        final GradientDrawable gradientDrawable = build(COLOR_COMBINATIONS[(key.hashCode() & Integer.MAX_VALUE) % COLOR_COMBINATIONS.length]);
        return ImageUtils.createTransitionDrawable(mResources.getDrawable(R.drawable.placeholder), gradientDrawable);
    }

    private GradientDrawable build(int[] colorIds) {
        int[] colors = { mResources.getColor(colorIds[0]), mResources.getColor(colorIds[1]) };
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

}
