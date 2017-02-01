package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.Nullable;

public class SwipeToRemoveStyleAttributes {

    public final int textColor;
    public final float textSize;
    public final Optional<String> removeText;
    public final int backgroundColor;
    public final float textPaddingRight;
    public final Typeface font;

    SwipeToRemoveStyleAttributes(int textColor,
                                 float textSize,
                                 @Nullable String removeText,
                                 int backgroundColor,
                                 float textPaddingRight,
                                 Typeface font) {
        this.textColor = textColor;
        this.textSize = textSize;
        this.removeText = Optional.fromNullable(removeText);
        this.backgroundColor = backgroundColor;
        this.textPaddingRight = textPaddingRight;
        this.font = font;
    }

    public static SwipeToRemoveStyleAttributes from(Context context) {
        final TypedArray ta = context.obtainStyledAttributes(R.style.PlayQueue_Remove,
                                                             R.styleable.PlayQueueSwipeRemoveItem);
        final SwipeToRemoveStyleAttributes styleAttributes = new SwipeToRemoveStyleAttributes(
                ta.getColor(R.styleable.PlayQueueSwipeRemoveItem_android_textColor, Color.RED),
                ta.getDimensionPixelSize(R.styleable.PlayQueueSwipeRemoveItem_android_textSize, 40),
                ta.getString(R.styleable.PlayQueueSwipeRemoveItem_android_text),
                ta.getColor(R.styleable.PlayQueueSwipeRemoveItem_android_background, Color.BLACK),
                ta.getDimensionPixelSize(R.styleable.PlayQueueSwipeRemoveItem_android_paddingRight, 40),
                Typeface.createFromAsset(context.getAssets(),
                        ta.getString(R.styleable.PlayQueueSwipeRemoveItem_custom_font))
        );

        ta.recycle();
        return styleAttributes;
    }
}
