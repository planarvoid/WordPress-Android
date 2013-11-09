package com.soundcloud.android.explore;

import com.soundcloud.android.view.OptimisedImageView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;

/**
 * TODO, just combine the gradient into a custom attribute of OptimisedImageView
 */
public class UserBrowserImageView extends OptimisedImageView {

    private static final double GRADIENT_START_POSITION = .8;
    private static final int[] GRADIENT_COLORS = new int[]{Color.TRANSPARENT, 0x5F000000, 0x9F000000};
    private static final float[] GRADIENT_POSITIONS = new float[]{0, .6F, 1};

    private Paint mPaint = new Paint();

    public UserBrowserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            final int startY = top + (int) ((bottom - top) * GRADIENT_START_POSITION);
            final LinearGradient shader = new LinearGradient(0, startY, 0, bottom, GRADIENT_COLORS, GRADIENT_POSITIONS, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getDrawable() != null) {
            final Rect clipBounds = canvas.getClipBounds();
            Rect r = new Rect();
            r.left = clipBounds.left;
            r.right = clipBounds.right;
            r.top = (int) ((clipBounds.bottom - clipBounds.top) * GRADIENT_START_POSITION + clipBounds.top);
            r.bottom = clipBounds.bottom - 1;
            canvas.drawRect(r, mPaint);
        }
    }
}
