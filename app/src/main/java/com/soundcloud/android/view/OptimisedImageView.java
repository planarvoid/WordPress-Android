package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OptimisedImageView extends AppCompatImageView {

    private static final float[] GRADIENT_POSITIONS = {0, .6f, 1f};

    private static final boolean DEFAULT_SHOW_GRADIENT = false;
    private static final float DEFAULT_GRADIENT_START = .7f;
    private static final int DEFAULT_GRADIENT_START_COLOR = 0x5f000000;
    private static final int DEFAULT_GRADIENT_END_COLOR = 0xaa000000;

    private boolean showGradient;
    private float gradientStart;
    private int gradientStartColor;
    private int gradientEndColor;

    private Paint gradientPaint;
    private Rect gradientRect;
    private int[] gradientColors;

    private boolean shouldIgnoreNextRequestLayout;

    public OptimisedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
        if (showGradient) {
            initGradientDrawAllocations();
        }
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OptimisedImageView);
        showGradient = a.getBoolean(R.styleable.OptimisedImageView_showGradient, DEFAULT_SHOW_GRADIENT);
        gradientStart = a.getFloat(R.styleable.OptimisedImageView_gradientStart, DEFAULT_GRADIENT_START);
        gradientStartColor = a.getColor(R.styleable.OptimisedImageView_gradientStartColor,
                                        DEFAULT_GRADIENT_START_COLOR);
        gradientEndColor = a.getColor(R.styleable.OptimisedImageView_gradientEndColor, DEFAULT_GRADIENT_END_COLOR);
        a.recycle();
    }

    private void initGradientDrawAllocations() {
        gradientPaint = new Paint();
        gradientRect = new Rect();
        gradientColors = new int[]{Color.TRANSPARENT, gradientStartColor, gradientEndColor};
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (showGradient && changed) {
            generateGradient(top, bottom);
        }
    }

    private void generateGradient(int top, int bottom) {
        // We have to allocate the shader in onLayout since it depends on the View height
        final int startY = top + (int) ((bottom - top) * gradientStart);
        final LinearGradient shader = new LinearGradient(0, startY, 0, bottom, gradientColors, GRADIENT_POSITIONS,
                                                         Shader.TileMode.CLAMP);
        gradientPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showGradient && getDrawable() != null) {
            drawGradient(canvas);
        }
    }

    private void drawGradient(Canvas canvas) {
        canvas.getClipBounds(gradientRect);
        int clipBoundsTop = gradientRect.top;
        gradientRect.top = (int) ((gradientRect.bottom - clipBoundsTop) * gradientStart + clipBoundsTop);
        canvas.drawRect(gradientRect, gradientPaint);
    }

    @Override
    public void requestLayout() {
        if (!shouldIgnoreNextRequestLayout) {
            super.requestLayout();
        }

        // Reset Flag so that the requestLayout() will work again
        shouldIgnoreNextRequestLayout = false;
    }

}
