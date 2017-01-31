package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * Offsets the imageView source by 20% from the top
 */
public class ParallaxImageView extends AspectRatioImageView {

    private final float focalPoint;
    private final int movement;
    private int parallaxOffset;

    public ParallaxImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ParallaxImageView);
        focalPoint = a.getFloat(R.styleable.ParallaxImageView_focalPoint, .5f);
        movement = (int) a.getDimension(R.styleable.ParallaxImageView_movement,
                                        -(int) (30 * context.getResources().getDisplayMetrics().density));
        a.recycle();
        setScaleType(ScaleType.MATRIX);
    }

    public void setParallaxOffset(double offset) {
        parallaxOffset = (int) (offset * movement);
        setFrame(getLeft(), getTop(), getRight(), getBottom());
        invalidate();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            final Matrix matrix = getImageMatrix();
            float scaleFactor = (r - l) / (float) drawable.getIntrinsicWidth();
            final int desiredFocalPoint = (int) (-(drawable.getIntrinsicHeight()) * focalPoint);
            matrix.setTranslate(0, desiredFocalPoint);
            matrix.postScale(scaleFactor, scaleFactor, 0, 0);
            matrix.postTranslate(0, Math.min((b - t) / (2) + parallaxOffset, -desiredFocalPoint * scaleFactor));
            setImageMatrix(matrix);
        }
        return super.setFrame(l, t, r, b);
    }
}
