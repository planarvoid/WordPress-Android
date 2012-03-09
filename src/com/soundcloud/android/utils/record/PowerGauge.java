
package com.soundcloud.android.utils.record;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

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

    private ArrayList<Float> mAllAmplitudes = new ArrayList<Float>();


    private long mShowFullStartTime;
    private boolean mShowFullAnimating;
    private static long SHOW_FULL_ANIMATE_TIME = 500;
    private int mFrameCount;
    private static AccelerateDecelerateInterpolator SHOW_FULL_INTERPOLATOR = new AccelerateDecelerateInterpolator();

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
        mAllAmplitudes.clear();
        nextBufferX = 0;
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = null;
    }



    public void showAll(){
        mShowFullAnimating = true;
        mShowFullStartTime = System.currentTimeMillis();

        nextBufferX = 0;
        if (bitmap != null) {
            bitmap.recycle();
        }
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mFrameCount = 0;
    }

    private void drawFullWave(Canvas c) {
        mFrameCount++;
        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mShowFullStartTime)) /
                SHOW_FULL_ANIMATE_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);

        int xIndex = 0;
        final int width = getWidth();
        int end = width;
        List<Float> amplitudesSubArray;
        final int amplitudeSize = mAllAmplitudes.size();

        if (amplitudeSize < width){
            final int gap = width - amplitudeSize;
            end = (int) (amplitudeSize + gap * interpolatedTime);
            amplitudesSubArray = mAllAmplitudes;
        } else {
            final int start = (int) (amplitudeSize - width - (interpolatedTime * (amplitudeSize - width)));
            amplitudesSubArray = mAllAmplitudes.subList(start, amplitudeSize);
        }

        final int amplitudeSubArraySize = amplitudesSubArray.size();
        for (xIndex = 0; xIndex < end; xIndex++) {
            drawAmplitude(c,xIndex,amplitudesSubArray.get((int)((((float )xIndex) /end) * amplitudeSubArraySize)));
        }

        if (normalizedTime < 1.0f) {
            postInvalidate();
        } else {
            mShowFullAnimating = false;
            Log.i("asdf","Current frame count " + mFrameCount);
        }


    }

    private void drawAmplitude(Canvas c, int xIndex, float amplitude) {
        // draw blur
        mPath.reset();
        mPath.moveTo(xIndex, this.getHeight() / 2 - amplitude * mMaxWaveHeight / 2);
        mPath.lineTo(xIndex, this.getHeight() / 2 + amplitude * mMaxWaveHeight / 2);
        c.drawPath(mPath, mBlurPaint);

        // draw amplitude
        c.drawLine(xIndex, this.getHeight() / 2 - amplitude * mMaxWaveHeight / 2,
                xIndex, this.getHeight() / 2 + amplitude * mMaxWaveHeight / 2, mPaint);
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
        if (mShowFullAnimating) {
            drawFullWave(canvas);
        } else if (bitmap != null) {
            if (nextBufferX > getWidth()) {
                m.setTranslate(getWidth() - nextBufferX, 0);
            } else {
                m.setTranslate(0, 0);
            }
            canvas.drawBitmap(bitmap, m, mPaint);
        }
    }

    public void updateAmplitude(float maxAmplitude) {
        mAllAmplitudes.add(maxAmplitude);

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
