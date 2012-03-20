package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.task.create.CalculateAmplitudesTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class CreateWaveView extends View{

    private static long ANIMATION_ZOOM_TIME = 500;

    private Bitmap bitmap;
    private int nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;
    private final Matrix m = new Matrix();
    private final Paint mBlurPaint = new Paint();
    private final Path mPath = new Path();

    private static final int WAVEFORM_DARK_UNPLAYED = 0xff444444;
    private static final int WAVEFORM_UNPLAYED = 0xffffffff;
    private static final int WAVEFORM_DARK_ORANGE = 0xff662000;
    private static final int WAVEFORM_ORANGE = 0xffff8000;

    private float mCurrentProgress;
    private double mSampleMax;
    private int mTrimLeft, mTrimRight;

    private int mMode;
    private boolean mInEditMode;

    private static final int MODE_ZOOM = 0;
    private static final int MODE_FULL = 1;

    private boolean mSized;
    private Paint mTrimLinePaint, mPlayedPaint, mUnplayedPaint,mDarkUnplayedPaint,mDarkPlayedPaint;

    private ArrayList<Float> mAllAmplitudes = new ArrayList<Float>();
    private int mRecordStartIndex = -1;
    private double[] mRealAmplitudes;

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

        mTrimLinePaint = new Paint();
        mTrimLinePaint.setColor(Color.GRAY);

        mPlayedPaint = new Paint();

        mUnplayedPaint = new Paint();
        mUnplayedPaint.setColor(WAVEFORM_UNPLAYED);

        mDarkUnplayedPaint = new Paint();
        mDarkUnplayedPaint.setColor(WAVEFORM_DARK_UNPLAYED);

        mDarkPlayedPaint = new Paint();
        mDarkPlayedPaint.setColor(WAVEFORM_DARK_ORANGE);
    }

    public void gotoPlaybackMode(){
        mCurrentProgress = -1;
        if (mMode != MODE_FULL){
            mMode = MODE_FULL;
            mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    public void gotoRecordMode(){
        if (mMode != MODE_ZOOM){
            mMode = MODE_ZOOM;
            mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    public void setPlaybackProgress(float progress){
        mCurrentProgress = progress;
        postInvalidate();
    }

    public void reset() {
        mAllAmplitudes.clear();

        mRecordStartIndex = -1;
        mCurrentProgress = -1f;
        mAnimationStartTime = -1l;
        nextBufferX = 0;
        mMode = MODE_ZOOM;
        mInEditMode = false;

        mTrimLeft = -1;
        mTrimRight = getWidth();

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        postInvalidate();
    }

    public void setInEditMode(boolean inEditMode) {
        mInEditMode = inEditMode;
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
                    canvas.drawBitmap(bitmap, m, mPlayedPaint);
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
        mTrimRight = w;

        LinearGradient lg =  new LinearGradient(0, 0, 0, mMaxWaveHeight,
            new int[]{
                    getResources().getColor(R.color.cloudProgressStart),
                    getResources().getColor(R.color.cloudProgressCenter),
                    getResources().getColor(R.color.cloudProgressEnd)
            },
            new float[]{0.0f,0.5f,1.0f},
            Shader.TileMode.MIRROR);

        mPlayedPaint.setShader(lg);
        mBlurPaint.setShader(lg);
    }


    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
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
            c.drawBitmap(old, mat, new Paint());

            nextBufferX = nextBufferX - old.getWidth() / 2;
            old.recycle();
        }

        if (isRecording && mRecordStartIndex == -1) {
            mRecordStartIndex = mAllAmplitudes.size()-1;
        }

        final Canvas c = new Canvas(bitmap);

        // draw blur
        if (isRecording) {
            c.drawLine(nextBufferX, this.getHeight() / 2 - maxAmplitude * mMaxWaveHeight / 2,
                nextBufferX, this.getHeight() / 2 + maxAmplitude * mMaxWaveHeight / 2, mBlurPaint);
        }

        // draw amplitude
        c.drawLine(nextBufferX, this.getHeight() / 2 - maxAmplitude * mMaxWaveHeight / 2,
                nextBufferX, this.getHeight() / 2 + maxAmplitude * mMaxWaveHeight / 2, isRecording ? mPlayedPaint : mDarkUnplayedPaint);

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
        mTrimLeft = -1;
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

    private void drawFullWave(Canvas c) {

        mFrameCount++;
        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mAnimationStartTime)) /
                ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);

        int startIndex = 0;
        final int width = getWidth();
        int endIndex = width;

        List<Float> amplitudesSubArray;

        // how many actual amplitudes do we want to display
        final int totalAmplitudeSize = mAllAmplitudes.size();
        final int recordedAmplitudeSize = mAllAmplitudes.size() - (mRecordStartIndex + 1);

        if (totalAmplitudeSize < width){
            startIndex = (int) (mRecordStartIndex - mRecordStartIndex * interpolatedTime);
            endIndex = (int) (totalAmplitudeSize + (width - totalAmplitudeSize) * interpolatedTime);
            amplitudesSubArray = mAllAmplitudes.subList(mRecordStartIndex, mAllAmplitudes.size()-1);
        } else if (recordedAmplitudeSize < width){
            final int gap = width - recordedAmplitudeSize;
            startIndex = (int) (gap - gap * interpolatedTime);
            amplitudesSubArray = mAllAmplitudes.subList(mRecordStartIndex, mAllAmplitudes.size()-1);
        } else {
            final int start = mRecordStartIndex + (int) (recordedAmplitudeSize - width - (interpolatedTime * (recordedAmplitudeSize - width)));
            amplitudesSubArray = mAllAmplitudes.subList(start, mAllAmplitudes.size()-1);
        }
        boolean animating = (normalizedTime < 1.0f);

        if (animating){
            // draw all orange
            drawPointsOnCanvas(c,amplitudesSubArray,startIndex,endIndex,mPlayedPaint, mBlurPaint);
        } else {
            final int currentProgressIndex = (int) (getWidth()*mCurrentProgress);

            if (!mInEditMode){
                // just draw progress (full orange if no current progress)
                if (currentProgressIndex < 0) {
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mPlayedPaint, mBlurPaint);
                } else {
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mPlayedPaint, mBlurPaint, 0, currentProgressIndex);
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mUnplayedPaint, null, currentProgressIndex, -1);
                }
            } else {

                // left handle
                drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mDarkPlayedPaint, null, 0, Math.max(mTrimLeft-1,0));
                drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mTrimLinePaint, null, Math.max(mTrimLeft-1,0), Math.max(mTrimLeft,1));

                // progress inside handles
                if (currentProgressIndex < 0) {
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mPlayedPaint, mBlurPaint, Math.max(mTrimLeft,1),mTrimRight-1);
                } else {
                    final int playMin = Math.max(mTrimLeft + 1, currentProgressIndex);
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mPlayedPaint, mBlurPaint, mTrimLeft + 1, playMin);
                    drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mUnplayedPaint, null, Math.min(mTrimRight -1, Math.max(playMin,currentProgressIndex)),mTrimRight-1);
                }

                // right handle
                drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mTrimLinePaint, null, mTrimRight-1, Math.min(endIndex,mTrimRight));
                drawPointsOnCanvas(c, amplitudesSubArray, startIndex, endIndex, mDarkUnplayedPaint, null, Math.min(endIndex,mTrimRight), -1);
            }

        }

        if (animating) postInvalidate();
    }

    private void drawPointsOnCanvas(Canvas c, List<Float> amplitudesArray, int startIndex, int endIndex, Paint paint, Paint blurPaint){
        drawPointsOnCanvas(c,amplitudesArray,startIndex,endIndex,paint, blurPaint, 0,-1);
    }

    private void drawPointsOnCanvas(Canvas c, List<Float> amplitudesArray, int startIndex, int endIndex, Paint paint, Paint blurPaint, int offsetLineIndex, int lastLineIndex){
        final float[] pts = new float[(endIndex - startIndex + 1) * 4];
        int currentProgressIndex = (int) (getWidth() * mCurrentProgress);
        int ptIndex = 0;
        final int height = getHeight();
        for (int x = startIndex; x <= endIndex; x++) {
            final float a = amplitudesArray.get((int) Math.min(amplitudesArray.size() - 1, ((float) (x - startIndex)) / (endIndex - startIndex) * amplitudesArray.size()));
            pts[ptIndex] = x;
            pts[ptIndex + 1] = height / 2 - a * mMaxWaveHeight / 2;
            pts[ptIndex + 2] = x;
            pts[ptIndex + 3] = height / 2 + a * mMaxWaveHeight / 2;
            ptIndex += 4;

        }

        final int pointOffset = offsetLineIndex * 4;
        final int pointCount = (lastLineIndex == -1 ? pts.length : lastLineIndex * 4) - pointOffset;
        c.drawLines(pts,pointOffset, pointCount, paint);
        // blur slows it down animation a lot. maybe put this back in later if we can make it more efficient
        //if (blurPaint != null) c.drawLines(pts, pointOffset, pointCount, blurPaint);
    }

    private static class Configuration {
        CalculateAmplitudesTask calculateAmplitudesTask;
        Bitmap bitmap;
    }
}

