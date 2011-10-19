
package com.soundcloud.android.utils.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.View;

public class PowerGauge extends View {

    private static final String TAG = "PowerGauge";

    private Bitmap bitmap;
    private float nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;
    private final Matrix m = new Matrix();
    private final Paint mPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private static final int WAVEFORM_ORANGE = 0xffff8000;
    private final Path mPath = new Path();

    public PowerGauge(Context context) {
        super(context);

        mPaint.setColor(WAVEFORM_ORANGE);

        mGlowHeight = (int) (5 * context.getResources().getDisplayMetrics().density);

        mBlurPaint.setAntiAlias(true);
        mBlurPaint.setDither(true);
        mBlurPaint.setColor(WAVEFORM_ORANGE);
        mBlurPaint.setStyle(Paint.Style.STROKE);
        mBlurPaint.setStrokeJoin(Paint.Join.ROUND);
        mBlurPaint.setStrokeCap(Paint.Cap.SQUARE);
        mBlurPaint.setStrokeWidth(1);
        mBlurPaint.setMaskFilter(new BlurMaskFilter(
                mGlowHeight, BlurMaskFilter.Blur.OUTER));

    }

    public void clear() {
        nextBufferX = 0;
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && this.getHeight() > 0) {
            mMaxWaveHeight = getHeight() - mGlowHeight;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            if (nextBufferX > getWidth()) {
                m.setTranslate(getWidth() - nextBufferX, 0);
            } else {
                m.setTranslate(0, 0);
            }
            canvas.drawBitmap(bitmap, m, mPaint);
        }
    }

    public void updateAmplitude(float maxAmplitude) {

        if (getWidth() == 0 || getHeight() == 0) { return; }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth() * 2, getHeight(), Bitmap.Config.ARGB_8888);
        } else if (nextBufferX + 1 > bitmap.getWidth()) {

            final Bitmap old = bitmap;
            bitmap = Bitmap.createBitmap(getWidth() * 2, getHeight(), Bitmap.Config.ARGB_8888);

            final Matrix mat = new Matrix();
            mat.setTranslate(-old.getWidth() / 2, 0);

            final Canvas c = new Canvas(bitmap);
            c.drawBitmap(old, mat, mPaint);

            nextBufferX = nextBufferX - old.getWidth() / 2;
            old.recycle();
        }

        final Canvas c = new Canvas(bitmap);

        // draw blur
        mPath.reset();
        mPath.moveTo(nextBufferX, this.getHeight() / 2 - maxAmplitude * mMaxWaveHeight / 2);
        mPath.lineTo(nextBufferX, this.getHeight() / 2 + maxAmplitude * mMaxWaveHeight / 2);
        c.drawPath(mPath, mBlurPaint);

        // draw amplitude
        c.drawLine(nextBufferX, this.getHeight() / 2 - maxAmplitude * mMaxWaveHeight / 2,
                nextBufferX, this.getHeight() / 2 + maxAmplitude * mMaxWaveHeight / 2, mPaint);

        nextBufferX++;

    }
}
