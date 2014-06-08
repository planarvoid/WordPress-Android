package com.soundcloud.android.creators.record;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

public class CreateWaveView extends View {
    private static final long ANIMATION_ZOOM_TIME = 400;
    private static final Interpolator SHOW_FULL_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    private final static Paint TRIM_LINE_PAINT;
    private final static Paint PLAYED_PAINT;
    private final static Paint UNPLAYED_PAINT;
    private final static Paint DARK_PAINT;
    private final static Paint BITMAP_PAINT;
    private final static Paint CLEAR_PAINT;

    private static final int WAVEFORM_TRIMMED = 0xff444444;
    private static final int WAVEFORM_UNPLAYED = 0xffcccccc;

    private Bitmap zoomBitmap1, zoomBitmap2;
    private int nextBitmapX = -1;
    private final int glowHeight;
    private int maxWaveHeight;

    private float currentProgress = -1f;

    private int mode;
    private boolean isEditing;

    private long animationStartTime;
    private float[] amplitudePoints;

    private final MergedAmplitudeData amplitudeData = new MergedAmplitudeData();
    private final DrawData drawData = new DrawData();

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

        CLEAR_PAINT = new Paint();
        CLEAR_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        CLEAR_PAINT.setColor(Color.TRANSPARENT);
    }

    public CreateWaveView(Context context) {
        super(context);
        glowHeight = (int) (5 * getContext().getResources().getDisplayMetrics().density);
        reset();
    }

    public void setMode(int mode, boolean animate) {
        if (this.mode != mode) {
            this.mode = mode;
            currentProgress = -1;

            if (this.mode == CreateWaveDisplay.MODE_REC) resetZoomBitmaps();

            if (animate) animationStartTime = System.currentTimeMillis();
            invalidate();
        }
    }

    public void setPlaybackProgress(float progress) {
        currentProgress = progress;
        invalidate();
    }

    public final void reset() {
        currentProgress = -1f;
        animationStartTime = -1l;
        mode = CreateWaveDisplay.MODE_REC;
        isEditing = false;

        resetZoomBitmaps();
        invalidate();
    }

    private void resetZoomBitmaps() {
        nextBitmapX = -1;
        if (zoomBitmap1 != null) {
            new Canvas(zoomBitmap1).drawRect(0, 0, zoomBitmap1.getWidth(), zoomBitmap1.getHeight(), CLEAR_PAINT);
        }
        if (zoomBitmap2 != null) {
            new Canvas(zoomBitmap2).drawRect(0, 0, zoomBitmap2.getWidth(), zoomBitmap2.getHeight(), CLEAR_PAINT);
        }
    }

    public void setIsEditing(boolean isEditing) {
        this.isEditing = isEditing;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        final SoundRecorder recorder = SoundRecorder.getInstance(getContext());
        if (recorder.isGeneratingWaveform()) return;

        final float[] trimWindow = recorder.getTrimWindow();
        final float normalizedTime = Math.min(1.0f, (((float) (System.currentTimeMillis() - animationStartTime)) / ANIMATION_ZOOM_TIME));
        final float interpolatedTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedTime);
        final boolean animating = (normalizedTime < 1.0f);

        amplitudeData.configure(recorder.getRecordStream(), trimWindow);
        drawData.configure(amplitudeData,
                interpolatedTime,
                mode == CreateWaveDisplay.MODE_REC,
                isEditing,
                getHeight(),
                getWidth());

        if (drawData.size > 0) {
            if (amplitudePoints == null) {
                // make sure we only allocate this array once - maximum points we're going to write
                amplitudePoints = new float[canvas.getWidth() * 4];
            }

            final int length = drawData.getAmplitudePoints(amplitudePoints, maxWaveHeight);

            if (animating) {
                if (drawData.recIndex == 0) {
                    // no prerecord data on screen so just draw it all in recording paint
                    canvas.drawLines(amplitudePoints, 0, length, PLAYED_PAINT);
                } else {
                    // mixed recording / prerecord data
                    final int recordStartIndex = (amplitudeData.writtenSize >= getWidth()) ? drawData.recIndex * 4
                            : Math.round(drawData.recIndex * ((float) drawData.lastDrawX) / drawData.size) * 4; // incorporate the scaling

                    canvas.drawLines(amplitudePoints, 0, recordStartIndex, DARK_PAINT);
                    canvas.drawLines(amplitudePoints, recordStartIndex, length - recordStartIndex, PLAYED_PAINT);
                }

            } else {
                if (mode == CreateWaveDisplay.MODE_REC) {
                    drawZoomView(canvas, drawData);
                } else {
                    drawFullView(canvas, amplitudePoints, length, trimWindow);
                }
            }
        }

        if (animating) invalidate();
    }

    /**
     * Draw the full version of the amplitude data (not currently animating or recording)
     * This will be subject to editing variables and playback progress
     *
     * @param canvas given by the view
     * @param points subset of amplitude data to be drawn
     */
    private void drawFullView(Canvas canvas, float[] points, int length, float[] trimWindow) {
        final int width = getWidth();

        if (!isEditing) {
            final int currentProgressIndex = (int) (currentProgress * width);
            // just draw progress (full orange if no current progress)
            if (currentProgressIndex < 0) {
                drawPointsOnCanvas(canvas, points, length, PLAYED_PAINT, 0, -1);
            } else {
                drawPointsOnCanvas(canvas, points, length, PLAYED_PAINT, 0, currentProgressIndex);
                drawPointsOnCanvas(canvas, points, length, UNPLAYED_PAINT, currentProgressIndex, -1);
            }

        } else {
            final int trimIndexLeft = (int) (trimWindow[0] * width);
            final int trimIndexRight = (int) (trimWindow[1] * width);
            int currentProgressIndex = currentProgress == -1 ? -1 :
                    (int) (trimIndexLeft + ((trimIndexRight - trimIndexLeft) * currentProgress));

            // left handle
            drawPointsOnCanvas(canvas, points, length, DARK_PAINT, 0, Math.max(trimIndexLeft - 1, 0));
            drawPointsOnCanvas(canvas, points, length, TRIM_LINE_PAINT, Math.max(trimIndexLeft - 1, 0), Math.max(trimIndexLeft, 1));

            // progress inside handles
            if (currentProgressIndex < 0) {
                drawPointsOnCanvas(canvas, points, length, PLAYED_PAINT, Math.max(trimIndexLeft, 1), trimIndexRight - 1);
            } else {

                final int playMin = Math.max(trimIndexLeft + 1, currentProgressIndex);

                drawPointsOnCanvas(canvas, points, length, PLAYED_PAINT, trimIndexLeft + 1, playMin);
                drawPointsOnCanvas(canvas, points, length, UNPLAYED_PAINT, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
            }

            // right handle
            drawPointsOnCanvas(canvas, points, length, TRIM_LINE_PAINT, trimIndexRight - 1, Math.min(width - 1, trimIndexRight));
            drawPointsOnCanvas(canvas, points, length, DARK_PAINT, Math.min(width - 1, trimIndexRight), -1);
        }
    }

    /**
     * Draw the zoomed in version of the amplitude (during recording) using the cached bitmaps
     *
     * @param c        the canvas
     * @param drawData in case we have to rebuild the bitmaps, this is our current amplitude data
     */
    private void drawZoomView(Canvas c, DrawData drawData) {
        final int width = getWidth();
        if (nextBitmapX == -1) {
            // draw current amplitudes
            Canvas bitmapCanvas = new Canvas(zoomBitmap1);
            final int drawCount = Math.min(width, drawData.size);

            for (nextBitmapX = 0; nextBitmapX < drawCount; nextBitmapX++) {
                final int index = drawData.size - drawCount + nextBitmapX;
                drawAmplitude(bitmapCanvas, nextBitmapX, drawData.get(index),
                        (drawData.recIndex == -1) || (index < drawData.recIndex) ? DARK_PAINT : PLAYED_PAINT);
            }
            nextBitmapX--;
        }
        // draw amplitudes cached to canvas
        Matrix m = new Matrix();
        if (nextBitmapX > getWidth()) {
            m.setTranslate(getWidth() - nextBitmapX + 1, 0);
            c.drawBitmap(zoomBitmap1, m, BITMAP_PAINT);
            m.setTranslate(getWidth() * 2 - nextBitmapX, 0);
            c.drawBitmap(zoomBitmap2, m, BITMAP_PAINT);
        } else {
            m.setTranslate(0, 0);
            c.drawBitmap(zoomBitmap1, m, BITMAP_PAINT);
        }

    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        if (width == 0 || height == 0) return;

        maxWaveHeight = height - glowHeight;
        LinearGradient lg = new LinearGradient(0, 0, 0, maxWaveHeight,
                new int[]{
                        getResources().getColor(R.color.cloud_progress_start),
                        getResources().getColor(R.color.cloud_progress_center),
                        getResources().getColor(R.color.cloud_progress_end)
                },
                new float[]{0.0f, 0.5f, 1.0f},
                Shader.TileMode.MIRROR);

        PLAYED_PAINT.setShader(lg);

        Bitmap old = zoomBitmap1;
        zoomBitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (old != null) old.recycle();

        old = zoomBitmap2;
        zoomBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (old != null) old.recycle();

        nextBitmapX = -1;
    }


    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        if (maxWaveHeight == 0) return;

        final int width = getWidth();
        if (nextBitmapX != -1 && zoomBitmap1 != null && zoomBitmap2 != null) {
            nextBitmapX++;
            if (nextBitmapX <= width) { // write to bitmap 1
                drawAmplitude(new Canvas(zoomBitmap1), nextBitmapX, maxAmplitude, isRecording ? PLAYED_PAINT : DARK_PAINT);

            } else if (nextBitmapX <= width * 2) { // write to bitmap 2
                drawAmplitude(new Canvas(zoomBitmap2), nextBitmapX - width, maxAmplitude, isRecording ? PLAYED_PAINT : DARK_PAINT);

            } else {
                // clear bitmap 1 and swap references
                Canvas c = new Canvas(zoomBitmap1);
                c.drawRect(0, 0, width, getHeight(), CLEAR_PAINT);

                final Bitmap old = zoomBitmap1;
                zoomBitmap1 = zoomBitmap2;
                zoomBitmap2 = old;

                nextBitmapX = width;
                drawAmplitude(new Canvas(zoomBitmap2), nextBitmapX, maxAmplitude, isRecording ? PLAYED_PAINT : DARK_PAINT);
            }

        }
        invalidate();
    }

    public void setCurrentProgress(float currentProgress) {
        this.currentProgress = currentProgress;
        invalidate();
    }

    private static void drawPointsOnCanvas(Canvas c, float[] points, int length, Paint paint, int offsetLineIndex, int lastLineIndex) {
        final int pointOffset = offsetLineIndex * 4;
        final int pointCount = (lastLineIndex == -1 ? length : lastLineIndex * 4) - pointOffset;

        c.drawLines(points, pointOffset, pointCount, paint);
    }

    private void drawAmplitude(Canvas c, int xIndex, float amplitude, Paint p) {
        c.drawLine(xIndex, this.getHeight() / 2 - amplitude * maxWaveHeight / 2,
                xIndex, this.getHeight() / 2 + amplitude * maxWaveHeight / 2, p);
    }

    public void onDestroy() {
        ImageUtils.clearBitmap(zoomBitmap1);
        ImageUtils.clearBitmap(zoomBitmap2);
        zoomBitmap1 = zoomBitmap2 = null;
    }

    /**
     * Merge the amplitudes available from the record stream and set some useful properties
     */
    static class MergedAmplitudeData {

        private AmplitudeData preRecData;
        private AmplitudeData recData;

        public int preRecSize;
        public int writtenSize;
        public int totalSize;

        public int recordStartIndexWithTrim;
        public int recordEndIndexWithTrim;

        public void configure(RecordStream recordStream, float[] trimWindow) {
            preRecData = recordStream.getPreRecordAmplitudeData();
            recData = recordStream.getAmplitudeData();

            preRecSize = preRecData == null ? 0 : preRecData.size();
            writtenSize = recData == null ? 0 : recData.size();
            totalSize = preRecSize + writtenSize;

            recordStartIndexWithTrim = (int) (preRecSize + trimWindow[0] * writtenSize);
            recordEndIndexWithTrim = (int) (totalSize - writtenSize * (1d - trimWindow[1]));
        }

        public float get(int i) {
            if (i < preRecData.size()) {
                return preRecData.get(i);
            } else {
                return recData.get(i - preRecData.size());
            }
        }
    }

    /**
     * This is a picture of what should be on screen, a subset of the available amplitude data that tells how it should be drawn
     */
    static class DrawData {

        private MergedAmplitudeData ampData;

        private int startIndex;
        private int endIndex;

        public int size;
        public int recIndex;
        public int lastDrawX;
        public int height;

        public DrawData() {
        }

        public void configure(MergedAmplitudeData mergedAmplitudeData, float interpolatedTime,
                              boolean isZooming,
                              boolean isEditing, int height, int width) {

            ampData = mergedAmplitudeData;
            this.height = height;

            // only show trim in recording
            final int absRecIndex = isEditing ? ampData.preRecSize : mergedAmplitudeData.recordStartIndexWithTrim;

            if (isZooming) {
                if (ampData.totalSize < width) {
                    startIndex = (int) (absRecIndex - absRecIndex * interpolatedTime);
                } else if (ampData.writtenSize < width) {
                    startIndex = absRecIndex - (int) ((width - ampData.writtenSize) * interpolatedTime);
                } else {
                    startIndex = Math.max(0, absRecIndex + (int) (interpolatedTime * (ampData.writtenSize - width)));
                }

            } else {
                if (ampData.totalSize < width) {
                    // all recorded data will always be on the screen, just interpolated the preview data out
                    startIndex = Math.max(0, (int) (absRecIndex * interpolatedTime));
                } else {
                    // interpolate all the recorded data on to the screen
                    final int gap = (ampData.totalSize - width) - absRecIndex;
                    startIndex = (int) Math.max(0, (ampData.totalSize - width) - gap * interpolatedTime);
                }
            }

            endIndex = isEditing ? ampData.totalSize : mergedAmplitudeData.recordEndIndexWithTrim;
            size = endIndex - startIndex;

            recIndex = Math.max(0, ampData.preRecSize - startIndex);

            if (isZooming) {
                lastDrawX = (size < width) ? (int) (width - (width - size) * interpolatedTime) : width;
            } else {
                lastDrawX = (size < width) ? (int) (size + (width - size) * interpolatedTime) : width;
            }
        }

        public float get(int i) {
            return ampData.get(i + startIndex);
        }

        public float getInterpolatedValue(int x, int width) {
            if (size > width) {
                // scaling down, nearest neighbor is fine
                return get((int) Math.min(size - 1, ((float) (x)) / width * size));
            } else {
                // scaling up, do interpolation
                final float fIndex = Math.min(size - 1, size * ((float) x) / (width));
                final float v1 = get((int) Math.floor(fIndex));
                final float v2 = get((int) Math.ceil(fIndex));
                return v1 + (v2 - v1) * (fIndex - ((int) fIndex));
            }
        }

        /**
         * @return number of data points
         */
        private int getAmplitudePoints(float[] points, int maxWaveHeight) {
            int ptIndex = 0;
            for (int x = 0; x < lastDrawX && ptIndex < points.length+3; x++) {
                final float a = size == lastDrawX ? get(x) : getInterpolatedValue(x, lastDrawX);
                points[ptIndex] = x;
                points[ptIndex + 1] = height / 2 - a * maxWaveHeight / 2;
                points[ptIndex + 2] = x;
                points[ptIndex + 3] = height / 2 + a * maxWaveHeight / 2;
                ptIndex += 4;
            }
            return lastDrawX * 4;
        }
    }
}

