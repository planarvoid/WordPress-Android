package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.task.create.CalculateAmplitudesTask;
import com.soundcloud.android.utils.record.CloudRecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

public class CreateWaveView extends View{

    private static long ANIMATION_ZOOM_TIME = 400;

    private Bitmap bitmap;
    private int nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;
    private final Matrix m = new Matrix();
    private final Path mPath = new Path();

    private static final int WAVEFORM_DARK_UNPLAYED = 0xff444444;
    private static final int WAVEFORM_UNPLAYED = 0xffffffff;
    private static final int WAVEFORM_DARK_ORANGE = 0xff662000;
    private static final int WAVEFORM_ORANGE = 0xffff8000;

    private float mCurrentProgress;
    private double mSampleMax;
    private int mTrimLeft, mTrimRight;

    private int mMode;
    private boolean mIsEditing;

    private boolean mSized;
    private Paint mTrimLinePaint, mPlayedPaint, mUnplayedPaint,mDarkUnplayedPaint,mDarkPlayedPaint;

    private List<Float> mAllAmplitudes;
    private int mRecordStartIndex = -1;

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

    public float[] getAmplitudes() {
        float[] floatArray = new float[mAllAmplitudes.size()];
        for (int i = 0; i < mAllAmplitudes.size(); i++) {
            Float f = mAllAmplitudes.get(i);
            floatArray[i] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;
    }

    public void setAmplitudes(float[] amplitudes) {
        mAllAmplitudes.clear();
        for (float amp : amplitudes) {
            mAllAmplitudes.add(amp);
        }
    }

    public void setMode(int mode, boolean animate){
        if (mMode != mode){
            mMode = mode;
            mCurrentProgress = -1;
            if (animate) mAnimationStartTime = System.currentTimeMillis();
            postInvalidate();
        }
    }

    public void setPlaybackProgress(float progress){
        mCurrentProgress = progress;
        postInvalidate();
    }

    public void reset() {
        mAllAmplitudes = null;
        mRecordStartIndex = -1;

        mCurrentProgress = -1f;
        mAnimationStartTime = -1l;
        nextBufferX = 0;
        mMode = CreateWaveDisplay.MODE_REC;
        mIsEditing = false;
        resetTrim();

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        postInvalidate();
    }

    public void resetTrim() {
        mTrimLeft = -1;
        mTrimRight = getWidth();
    }


    public void setIsEditing(boolean isEditing) {
        mIsEditing = isEditing;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (mMode) {
            case CreateWaveDisplay.MODE_REC :
                drawZoomWave(canvas);
                break;
            case CreateWaveDisplay.MODE_PLAYBACK:
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
    }


    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
         if (isRecording && mRecordStartIndex == -1) {
            mRecordStartIndex = CloudRecorder.getInstance().writeIndex;
        }
        postInvalidate();
    }

    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    public float getCurrentProgress() {
        return mCurrentProgress;
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

        assertAmplitudeHistory();

        if (mRecordStartIndex == -1) mRecordStartIndex = CloudRecorder.getInstance().writeIndex;

        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);

        boolean animating = (normalizedTime < 1.0f);

        final int width = getWidth();
        final int totalAmplitudeSize = mAllAmplitudes.size();
        final int recordedAmplitudeSize = mAllAmplitudes.size() - (mRecordStartIndex);

        int subArrayStart;
        if (totalAmplitudeSize < width) {
            subArrayStart = (int) (mRecordStartIndex * interpolatedTime);
        } else {
            final int gap = (totalAmplitudeSize - width) - mRecordStartIndex;
            subArrayStart = (int) Math.max(0,(totalAmplitudeSize - width) - gap * interpolatedTime);
        }

        final List<Float> amplitudesSubArray = mAllAmplitudes.subList(subArrayStart, mAllAmplitudes.size());
        if (amplitudesSubArray.size() > 0) {
            final int lastDrawX = (totalAmplitudeSize < width) ? (int) (totalAmplitudeSize + (width - totalAmplitudeSize) * interpolatedTime) : width;
            float[] points = getAmplitudePoints(amplitudesSubArray, 0, lastDrawX);
            if (animating) {
                if (mRecordStartIndex == -1) {
                    c.drawLines(points, mDarkUnplayedPaint);
                } else if (mRecordStartIndex <= subArrayStart) {
                    c.drawLines(points, mPlayedPaint);
                } else {
                    final int gap = (mRecordStartIndex - subArrayStart);
                    final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                            : Math.round(gap * ((float) lastDrawX) / amplitudesSubArray.size()) * 4; // incorporate the scaling

                    c.drawLines(points, 0, recordStartIndex, mDarkUnplayedPaint);
                    c.drawLines(points, recordStartIndex, points.length - recordStartIndex, mPlayedPaint);
                }

            } else {
                final int currentProgressIndex = (int) (getWidth() * mCurrentProgress);
                if (!mIsEditing) {
                    // just draw progress (full orange if no current progress)
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, mPlayedPaint);
                    } else {
                        drawPointsOnCanvas(c, points, mPlayedPaint, 0, currentProgressIndex);
                        drawPointsOnCanvas(c, points, mUnplayedPaint, currentProgressIndex, -1);
                    }
                } else {

                    // left handle
                    drawPointsOnCanvas(c, points, mDarkPlayedPaint, 0, Math.max(mTrimLeft - 1, 0));
                    drawPointsOnCanvas(c, points, mTrimLinePaint, Math.max(mTrimLeft - 1, 0), Math.max(mTrimLeft, 1));

                    // progress inside handles
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, mPlayedPaint, Math.max(mTrimLeft, 1), mTrimRight - 1);
                    } else {
                        final int playMin = Math.max(mTrimLeft + 1, currentProgressIndex);
                        drawPointsOnCanvas(c, points, mPlayedPaint, mTrimLeft + 1, playMin);
                        drawPointsOnCanvas(c, points, mUnplayedPaint, Math.min(mTrimRight - 1, Math.max(playMin, currentProgressIndex)), mTrimRight - 1);
                    }

                    // right handle
                    drawPointsOnCanvas(c, points, mTrimLinePaint, mTrimRight - 1, Math.min(width-1, mTrimRight));
                    drawPointsOnCanvas(c, points, mDarkUnplayedPaint, Math.min(width-1, mTrimRight), -1);
                }
            }

        }

        if (animating) postInvalidate();
    }



    private void drawZoomWave(Canvas c) {

        assertAmplitudeHistory();

        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);

        boolean animating = (normalizedTime < 1.0f);

        final int width = getWidth();
        final int totalAmplitudeSize = mAllAmplitudes.size();
        final int recordedAmplitudeSize = mAllAmplitudes.size() - (mRecordStartIndex);

        int subArrayStart;
        if (totalAmplitudeSize < width) {
            subArrayStart = (int) (mRecordStartIndex - mRecordStartIndex * interpolatedTime);
        } else if (recordedAmplitudeSize < width) {
            subArrayStart = mRecordStartIndex - (int) ((width - recordedAmplitudeSize) * interpolatedTime);
        } else {
            subArrayStart = Math.max(0,mRecordStartIndex + (int) (interpolatedTime * (recordedAmplitudeSize - width)));
        }

        final List<Float> amplitudesSubArray = mAllAmplitudes.subList(subArrayStart, mAllAmplitudes.size());
        if (amplitudesSubArray.size() > 0){
            final int lastDrawX = (totalAmplitudeSize < width) ? (int) (width - (width - totalAmplitudeSize) * interpolatedTime) : width;
            float[] points = getAmplitudePoints(amplitudesSubArray,0,lastDrawX);

            if (mRecordStartIndex == -1) {
                c.drawLines(points, mDarkUnplayedPaint);
            } else if (mRecordStartIndex <= subArrayStart){
                c.drawLines(points,mPlayedPaint);
            } else {
                final int gap = (mRecordStartIndex - subArrayStart);
                final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                        : Math.round(gap * ((float) lastDrawX) / amplitudesSubArray.size()) * 4; // incorporate the scaling

                c.drawLines(points,0,recordStartIndex,mDarkUnplayedPaint);
                c.drawLines(points,recordStartIndex,points.length-recordStartIndex, mPlayedPaint);
            }
        }
        if (animating) postInvalidate();
    }

    private void drawPointsOnCanvas(Canvas c, float[] points, Paint paint){
        drawPointsOnCanvas(c,points,paint, 0,-1);
    }

    private void drawPointsOnCanvas(Canvas c, float[] points, Paint paint, int offsetLineIndex, int lastLineIndex){
        final int pointOffset = offsetLineIndex * 4;
        final int pointCount = (lastLineIndex == -1 ? points.length-1 : lastLineIndex * 4) - pointOffset;
        c.drawLines(points,pointOffset, pointCount, paint);
    }

    private float[] getAmplitudePoints(List<Float> amplitudesArray, int firstDrawX, int lastDrawX) {

        final int amplitudesSize = amplitudesArray.size();
        final int height = getHeight();

        final float[] pts = new float[(lastDrawX - firstDrawX + 1) * 4];
        final boolean directSelect = amplitudesSize == (lastDrawX - firstDrawX);

        int currentProgressIndex = (int) (getWidth() * mCurrentProgress);
        int ptIndex = 0;
        for (int x = firstDrawX; x < lastDrawX; x++) {
            final float a = directSelect ? amplitudesArray.get(x - firstDrawX) : getInterpolatedAmpValue(amplitudesArray,amplitudesSize,x,firstDrawX,lastDrawX);
            pts[ptIndex] = x;
            pts[ptIndex + 1] = height / 2 - a * mMaxWaveHeight / 2;
            pts[ptIndex + 2] = x;
            pts[ptIndex + 3] = height / 2 + a * mMaxWaveHeight / 2;
            ptIndex += 4;
        }
        return pts;
    }

    private float getInterpolatedAmpValue(List<Float> amplitudesArray, int size, int x, int firstDrawX, int lastDrawX){
        if (size > lastDrawX - firstDrawX) {
            // scaling down, nearest neighbor is fine
            return amplitudesArray.get((int) Math.min(size - 1, ((float) (x - firstDrawX)) / (lastDrawX - firstDrawX) * size));
        } else {
            // scaling up, do interpolation
            final float fIndex = Math.min(size - 1, size * ((float) (x - firstDrawX)) / (lastDrawX - firstDrawX));
            final float v1 = amplitudesArray.get((int) Math.floor(fIndex));
            final float v2 = amplitudesArray.get((int) Math.ceil(fIndex));
            return v1 + (v2 - v1) * (fIndex - ((int) fIndex));
        }
    }

    private void assertAmplitudeHistory(){
        if (mAllAmplitudes == null) mAllAmplitudes = CloudRecorder.getInstance().amplitudes;
    }

    private static class Configuration {
        CalculateAmplitudesTask calculateAmplitudesTask;
        Bitmap bitmap;
    }
}

