package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.view.OverflowAnchorImageView;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

/**
 * Create a {@link R.attr#selectableItemBackground} instance which has @dimen/playlist_item_overflow_menu_padding padding for each side.
 */
class OverflowButtonBackground {

    private OverflowButtonBackground() {
    }

    public static void install(OverflowAnchorImageView button) {
        Context context = button.getContext();
        int insetSize = context.getResources().getDimensionPixelSize(R.dimen.playlist_item_overflow_menu_padding);
        Drawable backgroundDrawable = fetchSelectableItemBackgroundDrawable(context);
        button.setBackground(new InsetDrawable(backgroundDrawable, insetSize));
    }

    private static Drawable fetchSelectableItemBackgroundDrawable(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
        return ContextCompat.getDrawable(context, outValue.resourceId);
    }
}
