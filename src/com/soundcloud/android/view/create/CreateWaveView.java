package com.soundcloud.android.view.create;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import com.soundcloud.android.R;
import com.soundcloud.android.record.CloudRecorder;

import java.util.List;

public class CreateWaveView extends View {
    private static long ANIMATION_ZOOM_TIME = 400;

    private Bitmap mZoomBitmap;
    private int nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;

    private float mCurrentProgress;
    private int mTrimLeft, mTrimRight;

    private int mMode;
    private boolean mIsEditing;

    private List<Float> mAllAmplitudes;
    private int mRecordStartIndex = -1;


    private long mAnimationStartTime;

    private static final Interpolator SHOW_FULL_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private static Paint TRIM_LINE_PAINT;
    private static Paint PLAYED_PAINT;
    private static Paint UNPLAYED_PAINT;
    private static Paint DARK_UNPLAYED_PAINT;
    private static Paint DARK_PLAYED_PAINT;
    private static Paint BITMAP_PAINT;

    private static final int WAVEFORM_DARK_UNPLAYED = 0xff444444;
    private static final int WAVEFORM_UNPLAYED = 0xffffffff;
    private static final int WAVEFORM_DARK_ORANGE = 0xff662000;

    static {
        BITMAP_PAINT = new Paint();
        BITMAP_PAINT.setAntiAlias(true);

        TRIM_LINE_PAINT = new Paint();
        TRIM_LINE_PAINT.setColor(Color.GRAY);

        PLAYED_PAINT = new Paint();

        UNPLAYED_PAINT = new Paint();
        UNPLAYED_PAINT.setColor(WAVEFORM_UNPLAYED);

        DARK_UNPLAYED_PAINT = new Paint();
        DARK_UNPLAYED_PAINT.setColor(WAVEFORM_DARK_UNPLAYED);

        DARK_PLAYED_PAINT = new Paint();
        DARK_PLAYED_PAINT.setColor(WAVEFORM_DARK_ORANGE);
    }

    public CreateWaveView(Context context) {
        super(context);
        mGlowHeight = (int) (5 * getContext().getResources().getDisplayMetrics().density);
    }

    public void setMode(int mode, boolean animate){
        if (mMode != mode){
            mMode = mode;
            if (mZoomBitmap != null){
                mZoomBitmap.recycle();
                mZoomBitmap = null;
            }
            mCurrentProgress = -1;

            if (animate) mAnimationStartTime = System.currentTimeMillis();
            invalidate();

        }
    }

    public void setPlaybackProgress(float progress){
        mCurrentProgress = progress;
        invalidate();
    }

    public void reset() {
        mAllAmplitudes = null;
        mRecordStartIndex = -1;

        mCurrentProgress = -1f;
        mAnimationStartTime = -1l;
        mMode = CreateWaveDisplay.MODE_REC;
        mIsEditing = false;
        resetTrim();

        if (mZoomBitmap != null) {
            mZoomBitmap.recycle();
            mZoomBitmap = null;
        }

        invalidate();
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

        PLAYED_PAINT.setShader(lg);
    }


    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
         if (mMaxWaveHeight == 0) return;

        assertAmplitudeHistory();

        if (mZoomBitmap != null) {
            // if the new line would go over the edge, copy the last half of the old bitmap into the first half of the new bitmap
            if (nextBufferX + 1 > mZoomBitmap.getWidth()) {

                final Bitmap old = mZoomBitmap;
                mZoomBitmap = Bitmap.createBitmap(getWidth() * 2, getHeight(), Bitmap.Config.ARGB_8888);

                final Matrix mat = new Matrix();
                mat.setTranslate(-old.getWidth() / 2, 0);

                final Canvas c = new Canvas(mZoomBitmap);
                c.drawBitmap(old, mat, new Paint());

                nextBufferX = nextBufferX - old.getWidth() / 2;
                old.recycle();
            }

            drawAmplitude(new Canvas(mZoomBitmap), nextBufferX,maxAmplitude, isRecording ? PLAYED_PAINT : DARK_UNPLAYED_PAINT);
            nextBufferX++;
        }

        if (isRecording && mRecordStartIndex == -1) {
            mRecordStartIndex = CloudRecorder.getInstance(getContext()).writeIndex;
        }
        invalidate();
    }

    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
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

        if (mRecordStartIndex == -1) mRecordStartIndex = CloudRecorder.getInstance(getContext()).writeIndex;

        float normalizedTime = Math.min(1.0f, (((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
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
                    c.drawLines(points, DARK_UNPLAYED_PAINT);
                } else if (mRecordStartIndex <= subArrayStart) {
                    c.drawLines(points, PLAYED_PAINT);
                } else {
                    final int gap = (mRecordStartIndex - subArrayStart);
                    final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                            : Math.round(gap * ((float) lastDrawX) / amplitudesSubArray.size()) * 4; // incorporate the scaling

                    c.drawLines(points, 0, recordStartIndex, DARK_UNPLAYED_PAINT);
                    c.drawLines(points, recordStartIndex, points.length - recordStartIndex, PLAYED_PAINT);
                }

            } else {

                final int currentProgressIndex = (int) (getWidth() * mCurrentProgress);
                if (!mIsEditing) {
                    // just draw progress (full orange if no current progress)
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT);
                    } else {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, 0, currentProgressIndex);
                        drawPointsOnCanvas(c, points, UNPLAYED_PAINT, currentProgressIndex, -1);
                    }
                } else {

                    // left handle
                    drawPointsOnCanvas(c, points, DARK_PLAYED_PAINT, 0, Math.max(mTrimLeft - 1, 0));
                    drawPointsOnCanvas(c, points, TRIM_LINE_PAINT, Math.max(mTrimLeft - 1, 0), Math.max(mTrimLeft, 1));

                    // progress inside handles
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, Math.max(mTrimLeft, 1), mTrimRight - 1);
                    } else {
                        final int playMin = Math.max(mTrimLeft + 1, currentProgressIndex);
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, mTrimLeft + 1, playMin);
                        drawPointsOnCanvas(c, points, UNPLAYED_PAINT, Math.min(mTrimRight - 1, Math.max(playMin, currentProgressIndex)), mTrimRight - 1);
                    }

                    // right handle
                    drawPointsOnCanvas(c, points, TRIM_LINE_PAINT, mTrimRight - 1, Math.min(width-1, mTrimRight));
                    drawPointsOnCanvas(c, points, DARK_UNPLAYED_PAINT, Math.min(width-1, mTrimRight), -1);
                }
            }

        }

        if (animating) invalidate();
    }



    private void drawZoomWave(Canvas c) {

        if (mAllAmplitudes == null) return;

        float normalizedTime = Math.min(1.0f,(((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);
        final int width = getWidth();
        final int totalAmplitudeSize = mAllAmplitudes.size();


        boolean animating = (normalizedTime < 1.0f);

        if (animating){
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
                    c.drawLines(points, DARK_UNPLAYED_PAINT);
                } else if (mRecordStartIndex <= subArrayStart){
                    c.drawLines(points, PLAYED_PAINT);
                } else {
                    final int gap = (mRecordStartIndex - subArrayStart);
                    final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                            : Math.round(gap * ((float) lastDrawX) / amplitudesSubArray.size()) * 4; // incorporate the scaling

                    c.drawLines(points,0,recordStartIndex, DARK_UNPLAYED_PAINT);
                    c.drawLines(points,recordStartIndex,points.length-recordStartIndex, PLAYED_PAINT);
                }
            }
            invalidate();
        } else {
            if (mZoomBitmap == null){
                // draw current amplitudes
                mZoomBitmap = Bitmap.createBitmap(width * 2, getHeight(), Bitmap.Config.ARGB_8888);
                Canvas bitmapCanvas = new Canvas(mZoomBitmap);
                final int drawCount = Math.min(width,totalAmplitudeSize);

                for (nextBufferX = 0; nextBufferX < drawCount; nextBufferX++){
                    final int index = totalAmplitudeSize - drawCount + nextBufferX;
                    drawAmplitude(bitmapCanvas, nextBufferX,mAllAmplitudes.get(index),
                            (mRecordStartIndex == -1) || (index < mRecordStartIndex) ? DARK_UNPLAYED_PAINT : PLAYED_PAINT);
                }
            }
            // draw amplitudes cached to canvas
            Matrix m = new Matrix();
            if (nextBufferX > getWidth()) {
                m.setTranslate(getWidth() - nextBufferX, 0);
            } else {
                m.setTranslate(0, 0);
            }
            c.drawBitmap(mZoomBitmap, m, BITMAP_PAINT);
        }
    }

    private void drawPointsOnCanvas(Canvas c, float[] points, Paint paint){
        drawPointsOnCanvas(c, points, paint, 0, -1);
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
        if (mAllAmplitudes == null) mAllAmplitudes = CloudRecorder.getInstance(getContext()).amplitudes;
    }

    private void drawAmplitude(Canvas c, int xIndex, float amplitude, Paint p) {
        // draw amplitude
        c.drawLine(xIndex, this.getHeight() / 2 - amplitude * mMaxWaveHeight / 2,
                xIndex, this.getHeight() / 2 + amplitude * mMaxWaveHeight / 2, p);
    }
}

