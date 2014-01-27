package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OptimisedImageView extends ImageView {

    private Paint mPaint = new Paint();

    private boolean mIgnoreNextRequestLayout;
    private boolean mShowGradient;

    private double mGradientStart = .7;

    public OptimisedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setImageDrawable(final Drawable newDrawable) {
        if (newDrawable != null && VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {

            // The currently set Drawable
            final Drawable oldDrawable = getDrawable();

            if (null != oldDrawable && oldDrawable != newDrawable) {
                final int oldWidth = oldDrawable.getIntrinsicWidth();
                final int oldHeight = oldDrawable.getIntrinsicHeight();

                /*
                 * Ignore the next requestLayout call if the new Drawable is the
                 * same size as the currently displayed one.
                 */
                mIgnoreNextRequestLayout = oldHeight == newDrawable.getIntrinsicHeight()
                        && oldWidth == newDrawable.getIntrinsicWidth();
            }
        }

        // Finally, call up to super
        super.setImageDrawable(newDrawable);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mShowGradient && changed) {
            generateGradient(top, bottom);
        }
    }

    private void generateGradient(int top, int bottom) {
        final int[] colors = new int[]{Color.TRANSPARENT, 0x5F000000, 0xAA000000};
        final float[] positions = new float[]{0, .6F, 1};

        final int startY = top + (int) ((bottom - top) * mGradientStart);
        final LinearGradient shader = new LinearGradient(0, startY, 0, bottom, colors, positions, Shader.TileMode.CLAMP);
        mPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mShowGradient && getDrawable() != null) {
            drawGradient(canvas);
        }
    }

    private void drawGradient(Canvas canvas) {
        final Rect clipBounds = canvas.getClipBounds();
        Rect r = new Rect();
        r.left = clipBounds.left;
        r.right = clipBounds.right;
        r.top = (int) ((clipBounds.bottom - clipBounds.top) * mGradientStart + clipBounds.top);
        r.bottom = clipBounds.bottom;
        canvas.drawRect(r, mPaint);
    }

    @Override
    public void requestLayout() {
        if (!mIgnoreNextRequestLayout) {
            super.requestLayout();
        }

        // Reset Flag so that the requestLayout() will work again
        mIgnoreNextRequestLayout = false;
    }

}