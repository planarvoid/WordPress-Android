package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

/**
 * Draws a Drawable on top of the underlying image. Also allows the view to be square
 */
public class ForegroundImageView extends OptimisedImageView {

    private static final boolean DEFAULT_SQUARE = false;

    private boolean square = false;
    private Drawable foregroundDrawable;

    public ForegroundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ForegroundImageView);
        square = typedArray.getBoolean(R.styleable.ForegroundImageView_square, DEFAULT_SQUARE);
        int resourceId = typedArray.getResourceId(R.styleable.ForegroundImageView_foregroundDrawable, -1);
        foregroundDrawable = ContextCompat.getDrawable(context, resourceId);
        typedArray.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            foregroundDrawable.setBounds(0, 0, right - left, bottom - top);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        foregroundDrawable.draw(canvas);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (square) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}
