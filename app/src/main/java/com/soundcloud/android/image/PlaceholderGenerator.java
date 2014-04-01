package com.soundcloud.android.image;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
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

    private GradientDrawable[] mDrawables;

    @Inject
    PlaceholderGenerator(Resources resources) {
        mResources = resources;
        mDrawables = new GradientDrawable[COLOR_COMBINATIONS.length];
        for (int i = 0; i < COLOR_COMBINATIONS.length; i ++) {
            mDrawables[i] = build(COLOR_COMBINATIONS[i]);
        }
    }

    public Drawable generate(String indexerUrn) {
        // What is going on here? See: http://findbugs.blogspot.de/2006/09/is-mathabs-broken.html
        return mDrawables[(indexerUrn.hashCode() & Integer.MAX_VALUE) % mDrawables.length];
    }

    private GradientDrawable build(int[] colorIds) {
        int[] colors = { mResources.getColor(colorIds[0]), mResources.getColor(colorIds[1]) };
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

}
