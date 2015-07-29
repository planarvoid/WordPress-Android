package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CardFullImageView extends OptimisedImageView {

    private static final int RATIO = 2;
    private static final float FOCAL_POINT = .4F;
    private final Paint paint;

    public CardFullImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(getResources().getColor(R.color.card_full_image_border));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        final Drawable drawable = getDrawable();
        if (drawable != null){
            final Matrix matrix = getImageMatrix();
            float scaleFactor = (r-l)/(float) drawable.getIntrinsicWidth();
            final int desiredFocalPoint = (int) (-(drawable.getIntrinsicHeight()) * FOCAL_POINT);
            matrix.setTranslate(0, desiredFocalPoint);
            matrix.postScale(scaleFactor, scaleFactor, 0, 0);
            matrix.postTranslate(0, Math.min((b-t)/(2) + FOCAL_POINT, -desiredFocalPoint*scaleFactor));
            setImageMatrix(matrix);
        }
        return super.setFrame(l, t, r, b);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        try {
            Drawable drawable = getDrawable();
            if (drawable == null) {
                setMeasuredDimension(0, 0);
            } else {
                int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
                int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

                if (measuredHeight == 0) { // Height set to wrap_content
                    setMeasuredDimension(measuredWidth, measuredWidth / RATIO);

                } else if (measuredWidth == 0){ // Width set to wrap_content
                    int width = drawable.getIntrinsicWidth();
                    setMeasuredDimension(width, width / RATIO);

                } else { // Width and height are explicitly set (either to match_parent or to exact value)
                    setMeasuredDimension(measuredWidth, measuredHeight);
                }
            }
        } catch (Exception e) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}
