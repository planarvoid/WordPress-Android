package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.record.AmplitudeData;
import com.soundcloud.android.record.SoundRecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class CreateWaveView extends View {
    private static long ANIMATION_ZOOM_TIME = 400;
    private static final Interpolator SHOW_FULL_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private final static Paint TRIM_LINE_PAINT;
    private final static Paint PLAYED_PAINT;
    private final static Paint UNPLAYED_PAINT;
    private final static Paint DARK_PAINT;
    private final static Paint BITMAP_PAINT;

    private static final int WAVEFORM_TRIMMED   = 0xff444444;
    private static final int WAVEFORM_UNPLAYED  = 0xffcccccc;

    private Bitmap mZoomBitmap;
    private int nextBufferX;
    private int mGlowHeight;
    private int mMaxWaveHeight;

    private float mCurrentProgress = -1f;
    private double mTrimLeft, mTrimRight;

    private int mMode;
    private boolean mIsEditing;

    private AmplitudeData mAllAmplitudes;
    private int mRecordStartIndex = -1;

    private long mAnimationStartTime;

    static {
        BITMAP_PAINT = new Paint();
        BITMAP_PAINT.setAntiAlias(true);

        TRIM_LINE_PAINT = new Paint();
        TRIM_LINE_PAINT.setColor(Color.GRAY);

        PLAYED_PAINT = new Paint();

        UNPLAYED_PAINT = new Paint();
        UNPLAYED_PAINT.setColor(WAVEFORM_UNPLAYED);

        DARK_PAINT = new Paint();
        DARK_PAINT.setColor(WAVEFORM_TRIMMED);
    }

    public CreateWaveView(Context context) {
        super(context);
        mGlowHeight = (int) (5 * getContext().getResources().getDisplayMetrics().density);
    }

    public void setMode(int mode, boolean animate) {
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
        mTrimLeft = 0d;
        mTrimRight = 1d;
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

            drawAmplitude(new Canvas(mZoomBitmap), nextBufferX,maxAmplitude, isRecording ? PLAYED_PAINT : DARK_PAINT);
            nextBufferX++;
        }

        if (isRecording && mRecordStartIndex == -1) {
            mRecordStartIndex = SoundRecorder.getInstance(getContext()).writeIndex;
        }
        invalidate();
    }

    public void setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    public void setTrimLeft(double trimLeft) {
        mTrimLeft = trimLeft;
        invalidate();
    }

    public void setTrimRight(double trimRight) {
        mTrimRight = trimRight;
        invalidate();
    }

    private void drawFullWave(Canvas c) {
        assertAmplitudeHistory();

        if (mRecordStartIndex == -1) mRecordStartIndex = SoundRecorder.getInstance(getContext()).writeIndex;

        float normalizedTime = Math.min(1.0f, (((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);

        boolean animating = (normalizedTime < 1.0f);
        final int width = getWidth();
        final int totalAmplitudeSize = mAllAmplitudes.size();
        final int recordedAmplitudeSize = mAllAmplitudes.size() - (mRecordStartIndex);

        final int recordEndIndexWithTrim = mIsEditing ? totalAmplitudeSize : (int) (totalAmplitudeSize - recordedAmplitudeSize * (1d - mTrimRight));
        final int recordStartIndexWithTrim = mIsEditing ? mRecordStartIndex : (int) (mRecordStartIndex + mTrimLeft * recordedAmplitudeSize); //

        // figure out where in the amplitude array we should set our first index
        int start;
        if (totalAmplitudeSize < width) {
            // all recorded data will always be on the screen, just interpolated the preview data out
            start = Math.max(0, (int) (recordStartIndexWithTrim * interpolatedTime));
        } else {
            // interpolate all the recorded data on to the screen
            final int gap = (totalAmplitudeSize - width) - recordStartIndexWithTrim;
            start = (int) Math.max(0, (totalAmplitudeSize - width) - gap * interpolatedTime);
        }

        // this represents whatever should be on the screen now, taken from our calculated start position
        final AmplitudeData subData = mAllAmplitudes.slice(start, recordEndIndexWithTrim - start);

        // now figure out how to draw it
        if (subData.size() > 0) {

            // where should the last drawn X-coordinate be
            final int lastDrawX = (totalAmplitudeSize < width) ? (int) (totalAmplitudeSize + (width - totalAmplitudeSize) * interpolatedTime) : width;
            float[] points = getAmplitudePoints(subData, 0, lastDrawX);
            if (animating) {
                if (mRecordStartIndex == -1) {
                    c.drawLines(points, DARK_PAINT);
                } else if (mRecordStartIndex <= start) {
                    c.drawLines(points, PLAYED_PAINT);
                } else {
                    final int gap = (recordStartIndexWithTrim - start);
                    final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                            : Math.round(gap * ((float) lastDrawX) / subData.size()) * 4; // incorporate the scaling

                    c.drawLines(points, 0, recordStartIndex, DARK_PAINT);
                    c.drawLines(points, recordStartIndex, points.length - recordStartIndex, PLAYED_PAINT);
                }

            } else {

                if (!mIsEditing) {
                    final int currentProgressIndex = (int) (mCurrentProgress * width);
                    // just draw progress (full orange if no current progress)
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT);
                    } else {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, 0, currentProgressIndex);
                        drawPointsOnCanvas(c, points, UNPLAYED_PAINT, currentProgressIndex, -1);
                    }
                } else {
                    final int trimIndexLeft = (int) (mTrimLeft * width);
                    final int trimIndexRight = (int) (mTrimRight * width);
                    int currentProgressIndex = (int) (trimIndexLeft + ((trimIndexRight - trimIndexLeft)  * mCurrentProgress));

                    // left handle
                    drawPointsOnCanvas(c, points, DARK_PAINT, 0, Math.max(trimIndexLeft - 1, 0));
                    drawPointsOnCanvas(c, points, TRIM_LINE_PAINT, Math.max(trimIndexLeft - 1, 0), Math.max(trimIndexLeft, 1));

                    // progress inside handles
                    if (currentProgressIndex < 0) {
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, Math.max(trimIndexLeft, 1), trimIndexRight - 1);
                    } else {

                        final int playMin = Math.max(trimIndexLeft + 1, currentProgressIndex);
                        drawPointsOnCanvas(c, points, PLAYED_PAINT, trimIndexLeft + 1, playMin);
                        drawPointsOnCanvas(c, points, UNPLAYED_PAINT, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
                    }

                    // right handle
                    drawPointsOnCanvas(c, points, TRIM_LINE_PAINT, trimIndexRight - 1, Math.min(width-1, trimIndexRight));
                    drawPointsOnCanvas(c, points, DARK_PAINT, Math.min(width-1, trimIndexRight), -1);
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
            int start;
            if (totalAmplitudeSize < width) {
                start = (int) (mRecordStartIndex - mRecordStartIndex * interpolatedTime);
            } else if (recordedAmplitudeSize < width) {
                start = mRecordStartIndex - (int) ((width - recordedAmplitudeSize) * interpolatedTime);
            } else {
                start = Math.max(0,mRecordStartIndex + (int) (interpolatedTime * (recordedAmplitudeSize - width)));
            }

            final AmplitudeData amplitudesSubArray = mAllAmplitudes.slice(start, mAllAmplitudes.size() - start);
            if (amplitudesSubArray.size() > 0){
                final int lastDrawX = (totalAmplitudeSize < width) ? (int) (width - (width - totalAmplitudeSize) * interpolatedTime) : width;
                float[] points = getAmplitudePoints(amplitudesSubArray,0,lastDrawX);

                if (mRecordStartIndex == -1) {
                    c.drawLines(points, DARK_PAINT);
                } else if (mRecordStartIndex <= start){
                    c.drawLines(points, PLAYED_PAINT);
                } else {
                    final int gap = (mRecordStartIndex - start);
                    final int recordStartIndex = (recordedAmplitudeSize >= width) ? gap * 4
                            : Math.round(gap * ((float) lastDrawX) / amplitudesSubArray.size()) * 4; // incorporate the scaling

                    c.drawLines(points,0,recordStartIndex, DARK_PAINT);
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
                            (mRecordStartIndex == -1) || (index < mRecordStartIndex) ? DARK_PAINT : PLAYED_PAINT);
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

    private float[] getAmplitudePoints(AmplitudeData data, int firstDrawX, int lastDrawX) {

        final int amplitudesSize = data.size();
        final int height = getHeight();

        final float[] pts = new float[(lastDrawX - firstDrawX + 1) * 4];
        final boolean directSelect = amplitudesSize == (lastDrawX - firstDrawX);

        int ptIndex = 0;
        for (int x = firstDrawX; x < lastDrawX; x++) {
            final float a = directSelect ? data.get(x - firstDrawX) : data.getInterpolatedValue(x, firstDrawX, lastDrawX);
            pts[ptIndex] = x;
            pts[ptIndex + 1] = height / 2 - a * mMaxWaveHeight / 2;
            pts[ptIndex + 2] = x;
            pts[ptIndex + 3] = height / 2 + a * mMaxWaveHeight / 2;
            ptIndex += 4;
        }
        return pts;
    }


    private void assertAmplitudeHistory(){
        if (mAllAmplitudes == null) mAllAmplitudes = SoundRecorder.getInstance(getContext()).amplitudeData;
    }

    private void drawAmplitude(Canvas c, int xIndex, float amplitude, Paint p) {
        // draw amplitude
        c.drawLine(xIndex, this.getHeight() / 2 - amplitude * mMaxWaveHeight / 2,
                xIndex, this.getHeight() / 2 + amplitude * mMaxWaveHeight / 2, p);
    }
}

