package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import com.soundcloud.android.view.OverflowAnchorImageView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

/**
 * Create a {@link R.attr#selectableItemBackground} instance which has @dimen/playlist_item_overflow_menu_padding padding for each side.
 */
public final class OverflowButtonBackground {

    private OverflowButtonBackground() {
    }

    public static void install(OverflowAnchorImageView button, @DimenRes int insetSizeResId) {
        Context context = button.getContext();
        int insetSize = context.getResources().getDimensionPixelSize(insetSizeResId);
        Drawable backgroundDrawable = fetchSelectableItemBackgroundDrawable(context);
        button.setBackground(new InsetDrawable(backgroundDrawable, insetSize));
    }

    private static Drawable fetchSelectableItemBackgroundDrawable(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
        return ContextCompat.getDrawable(context, outValue.resourceId);
    }
}
