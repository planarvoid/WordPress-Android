package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
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

    private static final float GRADIENT_TOP = .6f;
    private static final float GRADIENT_BOTTOM = 1f;

    private static final boolean DEFAULT_SHOW_GRADIENT = false;
    private static final float DEFAULT_GRADIENT_START = .7f;
    private static final int DEFAULT_GRADIENT_START_COLOR = 0x5f000000;
    private static final int DEFAULT_GRADIENT_END_COLOR = 0xaa000000;

    private boolean mShowGradient;
    private float mGradientStart;
    private int mGradientStartColor;
    private int mGradientEndColor;
    private Paint mGradientPaint;

    private boolean mIgnoreNextRequestLayout;

    public OptimisedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
        if (mShowGradient) {
            mGradientPaint = new Paint();
        }
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OptimisedImageView);
        mShowGradient = a.getBoolean(R.styleable.OptimisedImageView_showGradient, DEFAULT_SHOW_GRADIENT);
        mGradientStart = a.getFloat(R.styleable.OptimisedImageView_gradientStart, DEFAULT_GRADIENT_START);
        mGradientStartColor = a.getColor(R.styleable.OptimisedImageView_gradientStartColor, DEFAULT_GRADIENT_START_COLOR);
        mGradientEndColor = a.getColor(R.styleable.OptimisedImageView_gradientEndColor, DEFAULT_GRADIENT_END_COLOR);
        a.recycle();
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
        final int[] colors = new int[]{Color.TRANSPARENT, mGradientStartColor, mGradientEndColor};
        final float[] positions = new float[]{0, GRADIENT_TOP, GRADIENT_BOTTOM};

        final int startY = top + (int) ((bottom - top) * mGradientStart);
        final LinearGradient shader = new LinearGradient(0, startY, 0, bottom, colors, positions, Shader.TileMode.CLAMP);
        mGradientPaint.setShader(shader);
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
        canvas.drawRect(r, mGradientPaint);
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