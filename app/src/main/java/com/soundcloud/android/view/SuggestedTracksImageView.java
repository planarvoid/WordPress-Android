package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Offsets the imageview source by 20% from the top
 * Used by {@link com.soundcloud.android.adapter.ExploreTracksAdapter}
 */
public class SuggestedTracksImageView extends AspectRatioImageView {

    public static final double GRADIENT_START_POSITION = .5;
    public static final int[] GRADIENT_COLORS = new int[]{Color.TRANSPARENT, 0x5F000000, 0x9F000000};
    public static final float[] GRADIENT_POSITIONS = new float[]{0, .6F, 1};
    private Paint mPaint = new Paint();

    public SuggestedTracksImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b)
    {
        final Drawable drawable = getDrawable();
        if (drawable != null){
            final Matrix matrix = getImageMatrix();
            float scaleFactor = (r-l)/(float) drawable.getIntrinsicWidth();
            final int desiredFocalPoint = (int) (-(drawable.getIntrinsicHeight()) * .35);
            matrix.setTranslate(0, desiredFocalPoint);
            matrix.postScale(scaleFactor, scaleFactor, 0, 0);
            matrix.postTranslate(0, (b-t)/(2));
            setImageMatrix(matrix);
        }
        return super.setFrame(l, t, r, b);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed){
            final int startY = top + (int) ((bottom - top) * GRADIENT_START_POSITION);
            final LinearGradient shader = new LinearGradient(0, startY, 0, bottom, GRADIENT_COLORS, GRADIENT_POSITIONS, Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getDrawable() != null){
            final Rect clipBounds = canvas.getClipBounds();
            Rect r = new Rect();
            r.left = clipBounds.left;
            r.right = clipBounds.right;
            r.top = (int) ((clipBounds.bottom - clipBounds.top) * GRADIENT_START_POSITION + clipBounds.top);
            r.bottom = clipBounds.bottom-1;
            canvas.drawRect(r, mPaint);
        }
    }
}
