package com.soundcloud.android.image;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import javax.inject.Inject;

public class CircularPlaceholderGenerator extends PlaceholderGenerator{

    @Inject
    CircularPlaceholderGenerator(Resources resources) {
        super(resources);
    }

    @Override
    protected Drawable getLoadingDrawable() {
        return resources.getDrawable(R.drawable.circular_placeholder);
    }

    @Override
    public GradientDrawable generateDrawable(String key) {
        final GradientDrawable gradientDrawable = super.generateDrawable(key);
        gradientDrawable.setShape(GradientDrawable.OVAL);
        return gradientDrawable;
    }

}
