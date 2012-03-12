package com.soundcloud.android.view.create;

import com.soundcloud.android.activity.CreateEditor;
import com.soundcloud.android.task.create.CalculateAmplitudesTask;
import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.utils.record.RawAudioPlayer;
import com.soundcloud.android.view.TouchLayout;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

public class CreateWaveView extends View{

    private static long ANIMATION_ZOOM_TIME = 500;

    private Bitmap bitmap;
    private int nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;
    private final Matrix m = new Matrix();
    private final Paint mPaint = new Paint();
    private final Paint mBlurPaint = new Paint();
    private static final int WAVEFORM_ORANGE = 0xffff8000;
    private final Path mPath = new Path();

    private static final int WAVEFORM_DARK_UNPLAYED = 0xff666666;
    private static final int WAVEFORM_UNPLAYED = 0xffffffff;
    private static final int WAVEFORM_DARK_PLAYED = 0xff662000;
    private static final int WAVEFORM_PLAYED = 0xffff8000;

    private float mCurrentProgress;
    private double mSampleMax;
    private int mTrimLeft, mTrimRight;


    private int mMode;


    private static final int MODE_FULL = 1;
    private static final int MODE_ZOOM = 2;

    private boolean mSized;
    private Paint mPlayedPaint, mUnplayedPaint,mDarkUnplayedPaint,mDarkPlayedPaint;

    private ArrayList<Float> mAllAmplitudes = new ArrayList<Float>();
    private double[] mRealAmplitudes;

    private boolean mAnimating;
    private TransitionListener mTransitionListener;
    public interface TransitionListener {
        void onFull();
        void onZoom();
    }

    private long mAnimationStartTime;

    private int mFrameCount;
    private static AccelerateDecelerateInterpolator SHOW_FULL_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    public CreateWaveView(Context context, TransitionListener listener) {
        super(context);

        mTransitionListener = listener;

        mPaint.setColor(WAVEFORM_ORANGE);

        mGlowHeight = (int) (5 * getContext().getResources().getDisplayMetrics().density);

        mBlurPaint.setAntiAlias(true);
        mBlurPaint.setDither(true);
        mBlurPaint.setColor(WAVEFORM_ORANGE);
        mBlurPaint.setStyle(Paint.Style.STROKE);
        mBlurPaint.setStrokeJoin(Paint.Join.ROUND);
        mBlurPaint.setStrokeCap(Paint.Cap.SQUARE);
        mBlurPaint.setStrokeWidth(1);
        mBlurPaint.setMaskFilter(new BlurMaskFilter(
                mGlowHeight, BlurMaskFilter.Blur.OUTER));

        mPlayedPaint = new Paint();
        mPlayedPaint.setColor(WAVEFORM_PLAYED);

        mUnplayedPaint = new Paint();
        mUnplayedPaint.setColor(WAVEFORM_UNPLAYED);

        mDarkUnplayedPaint = new Paint();
        mDarkUnplayedPaint.setColor(WAVEFORM_DARK_UNPLAYED);

        mDarkPlayedPaint = new Paint();
        mDarkPlayedPaint.setColor(WAVEFORM_DARK_PLAYED);
    }

    public void showFull(){
        if (mMode != MODE_FULL){
            mMode = MODE_FULL;
            mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    public void showZoom(){
        if (mMode != MODE_ZOOM){
            mMode = MODE_ZOOM;
            mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mMode) {
            case MODE_ZOOM :
                if (bitmap != null) {
                    if (nextBufferX > getWidth()) {
                        m.setTranslate(getWidth() - nextBufferX, 0);
                    } else {
                        m.setTranslate(0, 0);
                    }
                    canvas.drawBitmap(bitmap, m, mPaint);
                }
                break;
            case MODE_FULL:
                drawFullWave(canvas);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mMaxWaveHeight = h - mGlowHeight;
    }

    /*

    playback drawing logic

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        if (mAmplitudes != null){
            final int width = getWidth();
            final int height = getHeight();
            final int currentProgressIndex = (int) (width*mCurrentProgress);

            int i = 0;
            for (double amplitude : mAmplitudes) {
                final int halfWaveHeight = (int) ((amplitude / mSampleMax) * height / 2);
                if (i == mTrimLeft || i == mTrimRight -1) {
                    canvas.drawLine(i, 0, i, height, mUnplayedPaint);
                } else {
                    final Paint p = (i < mTrimLeft) ? mDarkPlayedPaint :
                                    ((i > mTrimRight) ? mDarkUnplayedPaint :
                                     (i >= currentProgressIndex) ? mUnplayedPaint : mPlayedPaint);

                    canvas.drawLine(i, height / 2 - halfWaveHeight, i, height / 2 + halfWaveHeight, p);
                }

                i++;
            }
        }
    }*/

    public void updateAmplitude(float maxAmplitude) {
        mAllAmplitudes.add(maxAmplitude);

        if (mMaxWaveHeight == 0) return;

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

        drawAmplitude(new Canvas(bitmap),nextBufferX,maxAmplitude);

        nextBufferX++;
        postInvalidate();

    }

    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    public float getCurrentProgress() {
        return mCurrentProgress;
    }

    public void setWave(double[] amplitudes, double sampleMax) {
        mRealAmplitudes = amplitudes;
        mSampleMax = sampleMax;
        invalidate();
        mTrimLeft = 0;
        mTrimRight = getWidth();
    }

    public void setTrimLeft(int trimLeft) {
        mTrimLeft = trimLeft;
        invalidate();
    }

    public void setTrimRight(int trimRight) {
        mTrimRight = trimRight;
        invalidate();
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

    private void drawFullWave(Canvas c) {
        mFrameCount++;
        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mAnimationStartTime)) /
                ANIMATION_ZOOM_TIME));
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

        if (normalizedTime < 1.0f) postInvalidate();
    }


    private static class Configuration {
        CalculateAmplitudesTask calculateAmplitudesTask;
        Bitmap bitmap;
    }
}

