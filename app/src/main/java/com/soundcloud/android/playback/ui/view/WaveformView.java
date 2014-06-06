package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.model.WaveformData;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {

    private static final int COLOR_ABOVE = 0xFFE5E5E5;
    private static final int COLOR_BELOW = 0xFFAFAFAF;

    private static final int BAR_WIDTH_DP = 2;
    private static final int BAR_SPACE_DP = 1;
    private static final int BASELINE_DP = 68;

    private final Paint aboveBackground;
    private final Paint belowBackground;
    private final Paint xorPaint;
    private final Paint xferInPaint;
    private final Paint antiAliasPaint;
    private final Rect cropRect;

    private final float barWidth;
    private final float spaceWidth;
    private final float baseline;

    private WaveformData data;
    private Bitmap waveformMask;
    private Bitmap target;

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        aboveBackground = new Paint();
        aboveBackground.setColor(COLOR_ABOVE);

        belowBackground = new Paint();
        belowBackground.setColor(COLOR_BELOW);

        xferInPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferInPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        xorPaint = new Paint();
        xorPaint.setColor(Color.WHITE);
        antiAliasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        final float density = getResources().getDisplayMetrics().density;
        barWidth = BAR_WIDTH_DP * density;
        spaceWidth = BAR_SPACE_DP * density;
        baseline = BASELINE_DP * density;

        cropRect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        target = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        buildWaveformMask();
    }

    public void setWaveform(WaveformData data) {
        this.data = data;
        buildWaveformMask();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (waveformMask == null || target == null) return;

        final int height = getHeight();
        final int width = getWidth();

        final Canvas targetCanvas = new Canvas(target);
        targetCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        targetCanvas.drawRect(0, 0, width, baseline, aboveBackground);
        targetCanvas.drawRect(0, baseline + spaceWidth, width, height, belowBackground);

        cropRect.set(0, 0, width, height);
        targetCanvas.drawBitmap(waveformMask, cropRect, cropRect, xferInPaint);
        canvas.drawBitmap(target, 0, 0, antiAliasPaint);
    }

    private void buildWaveformMask() {
        final int width = getWidth();
        final int height = getHeight();

        if (data == null || width == 0 || height == 0) {
            return;
        }

        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(mask);
        WaveformData scaled = data.scale(width);

        int acc = 0;
        int groupIndex = 0;
        int dumpIndex = -1;
        for (int i = 0; i < scaled.samples.length; i++) {
            if (dumpIndex >= 0) {
                dumpIndex++;
                if (dumpIndex == spaceWidth) {
                    dumpIndex = -1;
                }
            } else {
                acc += scaled.samples[i];
                groupIndex++;
                if (groupIndex == barWidth || i == scaled.samples.length - 1) {
                    final int sample = acc / groupIndex;
                    for (int j = i - groupIndex + 1; j <= i; j++) {
                        drawMaskAbove(canvas, j, sample * baseline / data.maxAmplitude);
                        drawMaskBelow(canvas, j, sample * (height - baseline) / data.maxAmplitude);
                    }
                    acc = groupIndex = dumpIndex = 0;
                }
            }
        }
        waveformMask = mask;
    }

    private void drawMaskAbove(Canvas canvas, int x, float height) {
        canvas.drawLine(x, baseline - height, x, baseline, xorPaint);
    }

    private void drawMaskBelow(Canvas canvas, int x, float height) {
        canvas.drawLine(x, baseline, x, baseline + height, xorPaint);
    }

}
