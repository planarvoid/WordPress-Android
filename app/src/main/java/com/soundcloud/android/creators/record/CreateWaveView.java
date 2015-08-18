package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;
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
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class CreateWaveView extends View {
    private static final long ANIMATION_ZOOM_TIME = 400;
    private static final int SPACE_ALPHA = 40;
    private static final Interpolator SHOW_FULL_INTERPOLATOR = new DecelerateInterpolator();

    private final static Paint BITMAP_PAINT;
    private final static Paint CLEAR_PAINT;

    private final Paint orangeBelowPaint;
    private final Paint orangeBelowSpacePaint;
    private final Paint grayAbovePaint;
    private final Paint grayAboveSpacePaint;
    private final Paint grayBelowPaint;
    private final Paint grayBelowSpacePaint;
    private final Paint darkAbovePaint;
    private final Paint darkBelowPaint;
    private final Paint trimLinePaint;
    private Paint orangeAbovePaint;
    private Paint orangeAboveSpacePaint;

    private final float baselineRatio;

    private Bitmap zoomBitmap1, zoomBitmap2;
    private int nextBitmapX = Consts.NOT_SET;

    private float currentProgress = Consts.NOT_SET;

    private int mode;
    private boolean isEditing;

    private long zoomAnimationStartTime;

    private float[] amplitudeAboveBarPoints;
    private float[] amplitudeBelowBarPoints;

    // We actual draw the spaces as semi-transparent lines, because if not the waveform will appear to flicker as it scrolls
    private float[] amplitudeAboveSpacePoints;
    private float[] amplitudeBelowSpacePoints;

    private final MergedAmplitudeData amplitudeData = new MergedAmplitudeData();
    private final DrawData drawData;
    private final CurrentAmplitudeHelper currentAmplitudeHelper;

    private final int orangeAboveStart;
    private final int orangeAboveEnd;


    static {
        BITMAP_PAINT = new Paint();
        BITMAP_PAINT.setAntiAlias(true);

        CLEAR_PAINT = new Paint();
        CLEAR_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        CLEAR_PAINT.setColor(Color.TRANSPARENT);
    }

    public CreateWaveView(Context context) {
        super(context);

        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.rec_waveform_baseline_ratio, outValue, true);
        baselineRatio = outValue.getFloat();

        orangeAboveStart = context.getResources().getColor(R.color.rec_wave_orange_above_start);
        orangeAboveEnd = context.getResources().getColor(R.color.rec_wave_orange_above_end);
        final int orangeAbove = context.getResources().getColor(R.color.rec_wave_orange_below);
        final int grayAbove = context.getResources().getColor(R.color.rec_wave_gray_above);
        final int grayBelow = context.getResources().getColor(R.color.rec_wave_gray_below);
        final int darkAbove = context.getResources().getColor(R.color.rec_wave_dark_gray_above);
        final int darkBelow = context.getResources().getColor(R.color.rec_wave_dark_gray_below);
        final int trimColor = context.getResources().getColor(R.color.rec_wave_trim_color);

        // sets color in onSizeChanged for the gradien
        orangeAbovePaint = new Paint();
        orangeAboveSpacePaint = new Paint();

        orangeBelowPaint = new Paint();
        orangeBelowPaint.setColor(orangeAbove);

        orangeBelowSpacePaint = new Paint();
        orangeBelowSpacePaint.setColor(orangeAbove);
        orangeBelowSpacePaint.setAlpha(SPACE_ALPHA);

        grayAbovePaint = new Paint();
        grayAbovePaint.setColor(grayAbove);

        grayAboveSpacePaint = new Paint();
        grayAboveSpacePaint.setColor(grayAbove);
        grayAboveSpacePaint.setAlpha(SPACE_ALPHA);

        grayBelowPaint = new Paint();
        grayBelowPaint.setColor(grayBelow);

        grayBelowSpacePaint = new Paint();
        grayBelowSpacePaint.setColor(grayBelow);
        grayBelowSpacePaint.setAlpha(SPACE_ALPHA);

        darkAbovePaint = new Paint();
        darkAbovePaint.setColor(darkAbove);

        darkBelowPaint = new Paint();
        darkBelowPaint.setColor(darkBelow);

        trimLinePaint = new Paint();
        trimLinePaint.setColor(trimColor);

        final int spaceWidth = context.getResources().getDimensionPixelSize(R.dimen.rec_waveform_space_width);
        final int barWidth = context.getResources().getDimensionPixelSize(R.dimen.rec_waveform_bar_width);

        drawData = new DrawData(baselineRatio, new CurrentAmplitudeHelper(barWidth, spaceWidth));
        currentAmplitudeHelper = new CurrentAmplitudeHelper(barWidth, spaceWidth);

        reset();
    }

    public void setMode(int mode, boolean animate) {
        if (this.mode != mode) {
            this.mode = mode;
            currentProgress = Consts.NOT_SET;

            if (animate) {
                zoomAnimationStartTime = System.currentTimeMillis();
            }
            invalidate();
        }
    }

    public void setPlaybackProgress(float progress) {
        currentProgress = progress;
        invalidate();
    }

    public final void reset() {
        currentProgress = Consts.NOT_SET;
        zoomAnimationStartTime = Consts.NOT_SET;
        mode = CreateWaveDisplay.MODE_REC;
        isEditing = false;

        resetZoomBitmaps();
        invalidate();
    }

    private void resetZoomBitmaps() {
        nextBitmapX = Consts.NOT_SET;
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
        final float[] trimWindow = recorder.getTrimWindow();
        final float normalizedZoomTime = Math.min(1.0f, (((float) (System.currentTimeMillis() - zoomAnimationStartTime)) / ANIMATION_ZOOM_TIME));
        final float interpolatedZoomTime = SHOW_FULL_INTERPOLATOR.getInterpolation(normalizedZoomTime);
        final boolean animatingZoom = (normalizedZoomTime < 1.0f);

        amplitudeData.configure(recorder.getRecordStream(), trimWindow);
        drawData.configure(amplitudeData,
                interpolatedZoomTime,
                mode == CreateWaveDisplay.MODE_REC,
                isEditing,
                getHeight(),
                getWidth());

        if (drawData.size > 0) {
            if (amplitudeAboveBarPoints == null) {
                // make sure we only allocate this array once - maximum points we're going to write
                final int pointCount = canvas.getWidth() * 4;
                amplitudeAboveBarPoints = new float[pointCount];
                amplitudeBelowBarPoints = new float[pointCount];
                amplitudeAboveSpacePoints = new float[pointCount];
                amplitudeBelowSpacePoints = new float[pointCount];
            }

            final int length = drawData.getAmplitudePoints(amplitudeAboveBarPoints, amplitudeBelowBarPoints,
                    amplitudeAboveSpacePoints, amplitudeBelowSpacePoints);

            if (animatingZoom) {
                if (drawData.recIndex == 0) {
                    // no prerecord data on screen so just draw it all in recording paint
                    canvas.drawLines(amplitudeAboveBarPoints, 0, length, orangeAbovePaint);
                    canvas.drawLines(amplitudeBelowBarPoints, 0, length, orangeBelowPaint);

                    canvas.drawLines(amplitudeAboveSpacePoints, 0, length, orangeAboveSpacePaint);
                    canvas.drawLines(amplitudeBelowSpacePoints, 0, length, orangeBelowSpacePaint);
                } else {
                    // mixed recording / prerecord data
                    final int recordStartIndex = (amplitudeData.writtenSize >= getWidth()) ? drawData.recIndex * 4
                            : Math.round(drawData.recIndex * ((float) drawData.lastDrawX) / drawData.size) * 4; // incorporate the scaling

                    canvas.drawLines(amplitudeAboveBarPoints, 0, recordStartIndex, grayAbovePaint);
                    canvas.drawLines(amplitudeBelowBarPoints, 0, recordStartIndex, grayBelowPaint);

                    canvas.drawLines(amplitudeAboveBarPoints, recordStartIndex, length - recordStartIndex, orangeAbovePaint);
                    canvas.drawLines(amplitudeBelowBarPoints, recordStartIndex, length - recordStartIndex, orangeBelowPaint);

                    canvas.drawLines(amplitudeAboveSpacePoints, 0, recordStartIndex, grayAboveSpacePaint);
                    canvas.drawLines(amplitudeBelowSpacePoints, 0, recordStartIndex, grayBelowSpacePaint);

                    canvas.drawLines(amplitudeAboveSpacePoints, recordStartIndex, length - recordStartIndex, orangeAboveSpacePaint);
                    canvas.drawLines(amplitudeBelowSpacePoints, recordStartIndex, length - recordStartIndex, orangeBelowSpacePaint);
                }

            } else {
                if (mode == CreateWaveDisplay.MODE_REC) {
                    drawZoomView(canvas, drawData);
                } else {
                    drawFullView(canvas, amplitudeAboveBarPoints, amplitudeBelowBarPoints, amplitudeAboveSpacePoints, amplitudeBelowSpacePoints, length, trimWindow);
                }
            }
        }

        if (animatingZoom) {
            invalidate();
        }
    }

    /**
     * Draw the full version of the amplitude data (not currently animating or recording)
     * This will be subject to editing variables and playback progress
     */
    private void drawFullView(Canvas canvas, float[] aboveBarPoints, float[] belowBarPoints, float[] aboveSpacePoints, float[] belowSpacePoints, int length, float[] trimWindow) {
        final int width = getWidth();

        if (!isEditing) {
            final int currentProgressIndex = (int) (currentProgress * width);

            // just draw progress (full orange if no current progress)
            if (currentProgressIndex < 0) {
                // orange bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, orangeAbovePaint, 0, Consts.NOT_SET);
                drawPointsOnCanvas(canvas, belowBarPoints, length, orangeBelowPaint, 0, Consts.NOT_SET);

                // orange spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, orangeAboveSpacePaint, 0, Consts.NOT_SET);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, orangeBelowSpacePaint, 0, Consts.NOT_SET);

            } else {

                // orange bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, orangeAbovePaint, 0, currentProgressIndex);
                drawPointsOnCanvas(canvas, belowBarPoints, length, orangeBelowPaint, 0, currentProgressIndex);

                // orange spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, orangeAboveSpacePaint, 0, currentProgressIndex);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, orangeBelowSpacePaint, 0, currentProgressIndex);

                // gray bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, grayAbovePaint, currentProgressIndex, Consts.NOT_SET);
                drawPointsOnCanvas(canvas, belowBarPoints, length, grayBelowPaint, currentProgressIndex, Consts.NOT_SET);

                // gray spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, grayAboveSpacePaint, currentProgressIndex, Consts.NOT_SET);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, grayBelowSpacePaint, currentProgressIndex, Consts.NOT_SET);
            }

        } else {
            final int trimIndexLeft = (int) (trimWindow[0] * width);
            final int trimIndexRight = (int) (trimWindow[1] * width);
            int currentProgressIndex = currentProgress == Consts.NOT_SET ? Consts.NOT_SET :
                    (int) (trimIndexLeft + ((trimIndexRight - trimIndexLeft) * currentProgress));

            // left points to cut
            drawPointsOnCanvas(canvas, aboveBarPoints, length, darkAbovePaint, 0, Math.max(trimIndexLeft - 1, 0));
            drawPointsOnCanvas(canvas, belowBarPoints, length, darkBelowPaint, 0, Math.max(trimIndexLeft - 1, 0));

            // progress inside handles
            if (currentProgressIndex < 0) {

                // orange bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, orangeAbovePaint, Math.max(trimIndexLeft, 1), trimIndexRight - 1);
                drawPointsOnCanvas(canvas, belowBarPoints, length, orangeBelowPaint, Math.max(trimIndexLeft, 1), trimIndexRight - 1);

                // orange spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, orangeAboveSpacePaint, Math.max(trimIndexLeft, 1), trimIndexRight - 1);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, orangeBelowSpacePaint, Math.max(trimIndexLeft, 1), trimIndexRight - 1);

            } else {

                final int playMin = Math.max(trimIndexLeft + 1, currentProgressIndex);

                // orange bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, orangeAbovePaint, trimIndexLeft + 1, playMin);
                drawPointsOnCanvas(canvas, belowBarPoints, length, orangeBelowPaint, trimIndexLeft + 1, playMin);

                // orange spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, orangeAboveSpacePaint, trimIndexLeft + 1, playMin);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, orangeBelowSpacePaint, trimIndexLeft + 1, playMin);

                // gray bars
                drawPointsOnCanvas(canvas, aboveBarPoints, length, grayAbovePaint, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
                drawPointsOnCanvas(canvas, belowBarPoints, length, grayBelowPaint, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);

                // gray spaces
                drawPointsOnCanvas(canvas, aboveSpacePoints, length, grayAboveSpacePaint, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
                drawPointsOnCanvas(canvas, belowSpacePoints, length, grayBelowSpacePaint, Math.min(trimIndexRight - 1, Math.max(playMin, currentProgressIndex)), trimIndexRight - 1);
            }

            // right points to cut
            drawPointsOnCanvas(canvas, aboveBarPoints, length, darkAbovePaint, Math.min(width - 1, trimIndexRight), Consts.NOT_SET);
            drawPointsOnCanvas(canvas, belowBarPoints, length, darkBelowPaint, Math.min(width - 1, trimIndexRight), Consts.NOT_SET);

            // left handle
            canvas.drawRect(0, 0, Math.max(trimIndexLeft, 1), getHeight(), trimLinePaint);

            // right handle
            canvas.drawRect(Math.max(trimIndexRight, 0), 0, width - 1, getHeight(), trimLinePaint);
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
        if (nextBitmapX == Consts.NOT_SET) {

            // draw current amplitudes
            Canvas bitmapCanvas = new Canvas(zoomBitmap1);
            final int drawCount = Math.min(width, drawData.size);

            for (nextBitmapX = 0; nextBitmapX < drawCount; nextBitmapX++) {
                final int index = drawData.size - drawCount + nextBitmapX;
                currentAmplitudeHelper.updateAmplitude(drawData.get(index));

                Paint abovePaint;
                Paint belowPaint;
                final boolean shouldShowSpace = currentAmplitudeHelper.shouldShowSpace();
                if (drawData.recIndex == Consts.NOT_SET || index < drawData.recIndex) {
                    abovePaint = shouldShowSpace ? grayAboveSpacePaint : grayAbovePaint;
                    belowPaint = shouldShowSpace ? grayBelowSpacePaint : grayBelowPaint;
                } else {
                    abovePaint = shouldShowSpace ? orangeAboveSpacePaint : orangeAbovePaint;
                    belowPaint = shouldShowSpace ? orangeBelowSpacePaint : orangeBelowPaint;
                }

                drawAmplitude(bitmapCanvas, nextBitmapX, currentAmplitudeHelper.currentValue(),abovePaint, belowPaint);
            }
            nextBitmapX--;
        }
        // draw amplitudes cached to canvas
        Matrix m = new Matrix();
        if (nextBitmapX > getWidth()) {
            m.setTranslate(getWidth() - nextBitmapX, 0);
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
        if (width != 0 && height != 0) {
            recreateZoomBitmaps(width, height);
            nextBitmapX = Consts.NOT_SET;
            orangeAbovePaint.setShader(new LinearGradient(0, 0, 0, baselineRatio * height, orangeAboveStart,
                    orangeAboveEnd, Shader.TileMode.MIRROR));

            orangeAboveSpacePaint.setShader(new LinearGradient(0, 0, 0, baselineRatio * height, orangeAboveStart,
                    orangeAboveEnd, Shader.TileMode.MIRROR));
            orangeAboveSpacePaint.setAlpha(SPACE_ALPHA);
        }
    }

    private void recreateZoomBitmaps(int width, int height) {
        if (zoomBitmap1 != null) {
            zoomBitmap1.recycle();
        }

        if (zoomBitmap2 != null) {
            zoomBitmap2.recycle();
        }
        zoomBitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoomBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }


    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        currentAmplitudeHelper.updateAmplitude(maxAmplitude);
        drawLastAmplitude(isRecording);
    }

    private void drawLastAmplitude(boolean isRecording) {
        if (getHeight() == 0) {
            return;
        }

        final int width = getWidth();
        if (nextBitmapX != Consts.NOT_SET && zoomBitmap1 != null && zoomBitmap2 != null) {
            nextBitmapX++;

            Paint abovePaint;
            Paint belowPaint;
            final boolean shouldShowSpace = currentAmplitudeHelper.shouldShowSpace();

            if (isRecording){
                abovePaint = shouldShowSpace ? orangeAboveSpacePaint : orangeAbovePaint;
                belowPaint = shouldShowSpace ? orangeBelowSpacePaint : orangeBelowPaint;
            } else {
                abovePaint = shouldShowSpace ? grayAboveSpacePaint : grayAbovePaint;
                belowPaint = shouldShowSpace ? grayBelowSpacePaint : grayBelowPaint;
            }

            float maxAmplitude = currentAmplitudeHelper.currentValue();
            if (nextBitmapX < width) { // write to bitmap 1
                drawAmplitude(new Canvas(zoomBitmap1), nextBitmapX, maxAmplitude, abovePaint, belowPaint);
            } else if (nextBitmapX < width * 2) { // write to bitmap 2
                drawAmplitude(new Canvas(zoomBitmap2), nextBitmapX - width, maxAmplitude, abovePaint, belowPaint);

            } else {
                // clear bitmap 1 and swap references
                Canvas c = new Canvas(zoomBitmap1);
                c.drawRect(0, 0, width, getHeight(), CLEAR_PAINT);

                final Bitmap old = zoomBitmap1;
                zoomBitmap1 = zoomBitmap2;
                zoomBitmap2 = old;

                nextBitmapX = width;
                drawAmplitude(new Canvas(zoomBitmap2), nextBitmapX - width, maxAmplitude, abovePaint, belowPaint);
            }
        }
        invalidate();
    }

    private static void drawPointsOnCanvas(Canvas c, float[] points, int length, Paint paint, int offsetLineIndex, int lastLineIndex) {
        final int pointOffset = offsetLineIndex * 4;
        final int pointCount = (lastLineIndex == Consts.NOT_SET ? length : lastLineIndex * 4) - pointOffset;
        c.drawLines(points, pointOffset, pointCount, paint);
    }

    private void drawAmplitude(Canvas c, int xIndex, float amplitude, Paint abovePaint, Paint belowPaint) {
        final float topHeight = this.getHeight() * baselineRatio;
        final float bottomHeight = this.getHeight() * (1-baselineRatio);
        c.drawLine(xIndex, topHeight - amplitude * topHeight, xIndex,topHeight, abovePaint);
        c.drawLine(xIndex, topHeight, xIndex, topHeight + amplitude * bottomHeight, belowPaint);
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
            } else if (i - preRecData.size() < recData.size() ) {
                return recData.get(i - preRecData.size());
            } else {
                return 0;
            }
        }
    }

    static class CurrentAmplitudeHelper {
        private float groupValue = Consts.NOT_SET;
        private float groupIndex = 0;
        private float dumpIndex = Consts.NOT_SET;
        private float accumulated = 0;
        private float accumulations = 0;

        private final float spaceWidth;
        private int barWidth;

        CurrentAmplitudeHelper(int barWidth, float spaceWidth) {
            this.barWidth = barWidth;
            this.spaceWidth = spaceWidth;
        }

        void reset(){
            groupValue = dumpIndex = Consts.NOT_SET;
            groupIndex = accumulated = accumulations = 0;
        }

        boolean shouldShowSpace(){
            return dumpIndex > 0;
        }

        float currentValue(){
            return groupValue;
        }

        void updateAmplitude(float lastAmplitude){
            accumulated += lastAmplitude;
            accumulations++;

            if (dumpIndex >= 0) {
                // the new index can be used to draw
                if (dumpIndex == 0){
                    calculateGroupValue();
                }

                // this will be a space
                dumpIndex++;
                if (dumpIndex == spaceWidth) {
                    dumpIndex = Consts.NOT_SET;
                }

            } else {
                // only set the new value if its gone up. this is for visual purposes only
                if (groupIndex == 0 && getCurrentAverage() > groupValue){
                    calculateGroupValue();
                }

                groupIndex++;
                if (groupIndex == barWidth){
                    groupIndex = dumpIndex = 0;
                }
            }
        }

        private void calculateGroupValue() {
            groupValue = getCurrentAverage();
            accumulated = accumulations = 0;
        }

        private float getCurrentAverage() {
            return accumulated / accumulations;
        }

    }

    /**
     * This is a picture of what should be on screen, a subset of the available amplitude data that tells how it should be drawn
     */
    static class DrawData {

        private final float baselineRatio;
        private final CurrentAmplitudeHelper currentAmplitudeHelper;
        private MergedAmplitudeData ampData;

        private int startIndex;
        private int endIndex;

        public int size;
        public int recIndex;
        public int lastDrawX;
        public int height;

        public DrawData(float baselineRatio, CurrentAmplitudeHelper currentAmplitudeHelper) {
            this.baselineRatio = baselineRatio;
            this.currentAmplitudeHelper = currentAmplitudeHelper;
        }

        @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
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
        private int getAmplitudePoints(float[] aboveBarPoints, float[] belowBarPoints, float[] aboveSpacePoints, float[] belowSpacePoints) {
            int ptIndex = 0;
            final float topHeight = height * baselineRatio;
            final float bottomHeight = height - height * baselineRatio;

            currentAmplitudeHelper.reset();

            for (int i = 0; i < lastDrawX && ptIndex < aboveBarPoints.length + 3; i++) {

                currentAmplitudeHelper.updateAmplitude(getInterpolatedValue(i, lastDrawX));

                final float updatedAmplitude = currentAmplitudeHelper.currentValue();

                float[] abovePoints = currentAmplitudeHelper.shouldShowSpace() ? aboveSpacePoints : aboveBarPoints;
                float[] belowPoints = currentAmplitudeHelper.shouldShowSpace() ? belowSpacePoints : belowBarPoints;

                abovePoints[ptIndex] = i;
                abovePoints[ptIndex + 1] = topHeight - updatedAmplitude * topHeight;
                abovePoints[ptIndex + 2] = i;
                abovePoints[ptIndex + 3] = topHeight;

                belowPoints[ptIndex] = i;
                belowPoints[ptIndex + 1] = topHeight;
                belowPoints[ptIndex + 2] = i;
                belowPoints[ptIndex + 3] = topHeight + updatedAmplitude * bottomHeight;
                ptIndex += 4;
            }
            return lastDrawX * 4;
        }
    }
}

