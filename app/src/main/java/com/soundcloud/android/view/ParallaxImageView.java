package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
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
public class ParallaxImageView extends AspectRatioImageView {

    // TODO, separate out gradient logic
    public static final double GRADIENT_START_POSITION = .5;
    public static final int[] GRADIENT_COLORS = new int[]{Color.TRANSPARENT, 0x5F000000, 0x9F000000};
    public static final float[] GRADIENT_POSITIONS = new float[]{0, .6F, 1};
    private Paint mPaint = new Paint();

    private float mFocalPoint;
    private int mMovement;
    private int mParallaxOffset;

    public ParallaxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ParallaxImageView);
        mFocalPoint = a.getFloat(R.styleable.ParallaxImageView_focalPoint, .5f);
        mMovement = (int) a.getDimension(R.styleable.ParallaxImageView_movement, -(int) (30 * context.getResources().getDisplayMetrics().density));
        a.recycle();

        setScaleType(ScaleType.MATRIX);
    }

    public void setParallaxOffset(double offset){
        mParallaxOffset = (int) (offset * mMovement);
        setFrame(getLeft(),getTop(),getRight(),getBottom());
        invalidate();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b)
    {
        final Drawable drawable = getDrawable();
        if (drawable != null){
            final Matrix matrix = getImageMatrix();
            float scaleFactor = (r-l)/(float) drawable.getIntrinsicWidth();
            final int desiredFocalPoint = (int) (-(drawable.getIntrinsicHeight()) * mFocalPoint);
            matrix.setTranslate(0, desiredFocalPoint);
            matrix.postScale(scaleFactor, scaleFactor, 0, 0);
            matrix.postTranslate(0, (b-t)/(2) + mParallaxOffset);
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
