package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.record.AmplitudeData;
import com.soundcloud.android.record.RecordStream;
import com.soundcloud.android.record.SoundRecorder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.FloatMath;
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


    private int mMode;
    private boolean mIsEditing;

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
        reset();
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
        mCurrentProgress = -1f;
        mAnimationStartTime = -1l;
        mMode = CreateWaveDisplay.MODE_REC;
        mIsEditing = false;

        if (mZoomBitmap != null) {
            mZoomBitmap.recycle();
            mZoomBitmap = null;
        }

        invalidate();
    }

    public void setIsEditing(boolean isEditing) {
        mIsEditing = isEditing;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        final SoundRecorder recorder = SoundRecorder.getInstance(getContext());
        final float[] trimWindow = recorder.getTrimWindow();
        if (recorder.isGeneratingWaveform()) return;

        float normalizedTime = Math.min(1.0f, (((float) (System.currentTimeMillis() - mAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);
        boolean animating = (normalizedTime < 1.0f);

        final MergedAmplitudeData amplitudeData = new MergedAmplitudeData(recorder.getRecordStream(), trimWindow);
        final DrawData drawData = new DrawData(amplitudeData, interpolatedTime, mMode == CreateWaveDisplay.MODE_REC, mIsEditing, getWidth());

        if (drawData.size > 0) {

            float[] points = getAmplitudePoints(drawData);
            if (animating) {
                if (drawData.recIndex == 0) {
                    // no prerecord data on screen so just draw it all in recording paint
                    canvas.drawLines(points, PLAYED_PAINT);
                } else {
                    // mixed recording / prerecord data
                    final int recordStartIndex = (amplitudeData.writtenSize >= getWidth()) ? drawData.recIndex * 4
                            : Math.round(drawData.recIndex * ((float) drawData.lastDrawX) / drawData.size) * 4; // incorporate the scaling

                    canvas.drawLines(points, 0, recordStartIndex, DARK_PAINT);
                    canvas.drawLines(points, recordStartIndex, points.length - recordStartIndex, PLAYED_PAINT);
                }

            } else {

                if (mMode == CreateWaveDisplay.MODE_REC){
                    drawZoomView(canvas,drawData);
                } else {
                    drawFullView(canvas,points, trimWindow);
                }

            }

        }

        if (animating) invalidate();
    }

    /**
     * Draw the full version of the amplitude data (not currently animating or recording)
     * This will be subject to editing variables and playback progress
     * @param canvas given by the view
     * @param points subset of amplitude data to be drawn
     */
    private void drawFullView(Canvas canvas, float[] points, float[] trimWindow) {
        final int width = getWidth();

        if (!mIsEditing) {
            final int currentProgressIndex = (int) (mCurrentProgress * width);
            // just draw progress (full orange if no current progress)
            if (currentProgressIndex < 0) {
                drawPointsOnCanvas(canvas, points, PLAYED_PAINT);
            } else {
                drawPointsOnCanvas(canvas, points, PLAYED_PAINT, 0, currentProgressIndex);
                drawPointsOnCanvas(canvas, points, UNPLAYED_PAINT, currentProgressIndex, -1);
            }

        } else {
            final int trimIndexLeft = (int) (trimWindow[0] * width);
            final int trimIndexRight = (int) (trimWindow[1] * width);
            int currentProgressIndex = mCurrentProgress == -1 ? -1 :
                    (int) (trimIndexLeft + ((trimIndexRight - trimIndexLeft) * mCurrentProgress));

            // left handle
            drawPointsOnCanvas(canvas, points, DARK_PAINT, 0, Math.max(trimIndexLeft - 1, 0));
            drawPointsOnCanvas(canvas, points, TRIM_LINE_PAINT, Math.max(trimIndexLeft - 1, 0), Math.max(trimIndexLeft, 1));

            // progress inside handles
            if (currentProgressIndex < 0) {
                drawPointsOnCanvas(canvas, points, PLAYED_PAINT, Math.max(trimIndexLeft, 1), trimIndexRight - 1);
            } else {

                final int playMin = Math.max(trimIndexLeft + 1, currentProgressIndex);

                drawPointsOnCanvas(canvas, points, PLAYED_PAINT, trimIndexLeft + 1, playMin);
                drawPointsOnCanvas(canvas, points, UNPLAYED_PAINT, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
            }

            // right handle
            drawPointsOnCanvas(canvas, points, TRIM_LINE_PAINT, trimIndexRight - 1, Math.min(width - 1, trimIndexRight));
            drawPointsOnCanvas(canvas, points, DARK_PAINT, Math.min(width - 1, trimIndexRight), -1);
        }
    }

    /**
     * Draw the zoomed in version of the amplitude (during recording) using the cached bitmaps
     * @param c the canvas
     * @param drawData in case we have to rebuild the bitmaps, this is our current amplitude data
     */
    private void drawZoomView(Canvas c, DrawData drawData) {
        final int width = getWidth();
        if (mZoomBitmap == null) {
            // draw current amplitudes
            mZoomBitmap = Bitmap.createBitmap(width * 2, getHeight(), Bitmap.Config.ARGB_8888);
            Canvas bitmapCanvas = new Canvas(mZoomBitmap);
            final int drawCount = Math.min(width, drawData.size);

            for (nextBufferX = 0; nextBufferX < drawCount; nextBufferX++) {
                final int index = drawData.size - drawCount + nextBufferX;
                drawAmplitude(bitmapCanvas, nextBufferX, drawData.get(index),
                        (drawData.recIndex == -1) || (index < drawData.recIndex) ? DARK_PAINT : PLAYED_PAINT);
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

        final RecordStream recordStream = SoundRecorder.getInstance(getContext()).getRecordStream();

        if (mZoomBitmap != null) {
            // if the new line would go over the edge, copy the last half of the old bitmap
            // into the first half of the new bitmap
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
        invalidate();
    }

    public void  setCurrentProgress(float currentProgress) {
        mCurrentProgress = currentProgress;
        invalidate();
    }

    private void drawPointsOnCanvas(Canvas c, float[] points, Paint paint){
        drawPointsOnCanvas(c, points, paint, 0, -1);
    }

    private void drawPointsOnCanvas(Canvas c, float[] points, Paint paint, int offsetLineIndex, int lastLineIndex){
        final int pointOffset = offsetLineIndex * 4;
        final int pointCount = (lastLineIndex == -1 ? points.length-1 : lastLineIndex * 4) - pointOffset;
        c.drawLines(points, pointOffset, pointCount, paint);
    }

    private float[] getAmplitudePoints(DrawData data) {

        final int amplitudesSize = data.size;
        final int height = getHeight();

        final float[] pts = new float[(data.lastDrawX + 1) * 4];
        final boolean directSelect = amplitudesSize == data.lastDrawX;

        int ptIndex = 0;
        for (int x = 0; x < data.lastDrawX; x++) {
            final float a = directSelect ? data.get(x) : data.getInterpolatedValue(x, data.lastDrawX);
            pts[ptIndex] = x;
            pts[ptIndex + 1] = height / 2 - a * mMaxWaveHeight / 2;
            pts[ptIndex + 2] = x;
            pts[ptIndex + 3] = height / 2 + a * mMaxWaveHeight / 2;
            ptIndex += 4;
        }
        return pts;
    }


    private void drawAmplitude(Canvas c, int xIndex, float amplitude, Paint p) {
        c.drawLine(xIndex, this.getHeight() / 2 - amplitude * mMaxWaveHeight / 2,
                xIndex, this.getHeight() / 2 + amplitude * mMaxWaveHeight / 2, p);
    }

    /**
     * Merge the amplitudes available from the record stream and set some useful properties
     */
    static class MergedAmplitudeData {

            private final AmplitudeData mPreRecData;
            private final AmplitudeData mRecData;

            public final int preRecSize;
            public final int writtenSize;
            public final int totalSize;

            public final int recordStartIndexWithTrim;
            public final int recordEndIndexWithTrim;

            MergedAmplitudeData(RecordStream recordStream, float[] trimWindow){

                mPreRecData = recordStream.getPreRecordAmplitudeData();
                mRecData = recordStream.getAmplitudeData();

                preRecSize = mPreRecData == null ? 0 : mPreRecData.size();
                writtenSize = mRecData == null ? 0 : mRecData.size();
                totalSize = preRecSize + writtenSize;

                recordStartIndexWithTrim = (int) (preRecSize + trimWindow[0] * writtenSize);
                recordEndIndexWithTrim = (int) (totalSize - writtenSize * (1d - trimWindow[1]));
            }

        public float get(int i) {
            if (i < mPreRecData.size()) {
                return mPreRecData.get(i);
            } else {
                return mRecData.get(i - mPreRecData.size());
            }
        }
    }

    /**
     * This is a picture of what should be on screen, a subset of the available amplitude data that tells how it should be drawn
     */
    static class DrawData {

        private final MergedAmplitudeData mAmpData;

        private final int startIndex;
        private final int endIndex;

        public final int size;
        public final int recIndex;
        public final int lastDrawX;

        public DrawData(MergedAmplitudeData mergedAmplitudeData, float interpolatedTime, boolean isZooming, boolean isEditing, int width) {

            mAmpData = mergedAmplitudeData;

            // only show trim in recording
            final int absRecIndex = isEditing ? mAmpData.preRecSize : mergedAmplitudeData.recordStartIndexWithTrim;

            if (isZooming){
                if (mAmpData.totalSize < width) {
                    startIndex = (int) (absRecIndex - absRecIndex * interpolatedTime);
                } else if (mAmpData.writtenSize < width) {
                    startIndex = absRecIndex - (int) ((width - mAmpData.writtenSize) * interpolatedTime);
                } else {
                    startIndex = Math.max(0, absRecIndex + (int) (interpolatedTime * (mAmpData.writtenSize - width)));
                }

            } else {
                if (mAmpData.totalSize < width) {
                    // all recorded data will always be on the screen, just interpolated the preview data out
                    startIndex = Math.max(0, (int) (absRecIndex * interpolatedTime));
                } else {
                    // interpolate all the recorded data on to the screen
                    final int gap = (mAmpData.totalSize - width) - absRecIndex;
                    startIndex = (int) Math.max(0, (mAmpData.totalSize - width) - gap * interpolatedTime);
                }
            }

            endIndex = isEditing ? mAmpData.totalSize : mergedAmplitudeData.recordEndIndexWithTrim;
            size = endIndex - startIndex;

            recIndex = Math.max(0,mAmpData.preRecSize - startIndex);

            if (isZooming){
                lastDrawX = (size < width) ? (int) (width - (width - size) * interpolatedTime) : width;
            } else {
                lastDrawX = (size < width) ? (int) (size + (width - size) * interpolatedTime) : width;
            }
        }

        public float get(int i) {
            return mAmpData.get(i+startIndex);
        }

        public float getInterpolatedValue(int x, int width) {
            if (size > width) {
                // scaling down, nearest neighbor is fine
                return get((int) Math.min(size - 1, ((float) (x)) / width * size));
            } else {
                // scaling up, do interpolation
                final float fIndex = Math.min(size - 1, size * ((float) x) / (width));
                final float v1 = get((int) FloatMath.floor(fIndex));
                final float v2 = get((int) FloatMath.ceil(fIndex));
                return v1 + (v2 - v1) * (fIndex - ((int) fIndex));
            }
        }
    }
}

