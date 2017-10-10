package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.waveform.WaveformData;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

public class WaveformCanvas extends View {
    private static final float MIN_UNPLAYABLE = 0.99f;

    private WaveformData waveformData;
    private Paint abovePaint;
    private Paint belowPaint;
    private Paint unplayableAbovePaint;
    private Paint unplayableBelowPaint;
    private int barWidth, spaceWidth, baseline;

    private float unplayableFromPosition = 1.0f;

    public WaveformCanvas(Context context) {
        this(context, null);
    }

    public WaveformCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void initialize(WaveformData waveformData,
                           float unplayableFromPosition,
                           Paint abovePaint,
                           Paint belowPaint,
                           Paint unplayableAbovePaint,
                           Paint unplayableBelowPaint,
                           int barWidth,
                           int spaceWidth,
                           int baseline) {
        this.waveformData = waveformData;
        this.abovePaint = abovePaint;
        this.belowPaint = belowPaint;
        this.barWidth = barWidth;
        this.spaceWidth = spaceWidth;
        this.baseline = baseline;
        this.unplayableAbovePaint = unplayableAbovePaint;
        this.unplayableBelowPaint = unplayableBelowPaint;
        this.unplayableFromPosition = unplayableFromPosition;
    }

    // Note: Progress events might have slightly less duration than metadata duration
    // but they should still be handled as fully-playable
    public void setUnplayableFromPosition(float unplayableFromPosition) {
        boolean isBelowFullPlayable = unplayableFromPosition < MIN_UNPLAYABLE;
        boolean wasBelowFullPlayable = this.unplayableFromPosition < MIN_UNPLAYABLE;

        if (isBelowFullPlayable != wasBelowFullPlayable) {
            this.unplayableFromPosition = unplayableFromPosition;
            invalidate();
        }
    }

    public void show() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        if (this.waveformData != null) {
            final int length = waveformData.samples.length;
            final int playableBars = (int) Math.min(length, Math.ceil(length * unplayableFromPosition));

            int w = barWidth + spaceWidth;
            int x = 0;
            float y;

            for (int bar = 0; bar < playableBars; bar++) {
                y = Math.max(spaceWidth, waveformData.samples[bar]);
                drawBar(canvas, x, y, abovePaint, belowPaint);
                x += w;
            }

            for (int bar = playableBars; bar < length; bar++) {
                y = Math.max(spaceWidth, waveformData.samples[bar]);
                drawBar(canvas, x, y, unplayableAbovePaint, unplayableBelowPaint);
                x += w;
            }
        }
    }

    private void drawBar(Canvas canvas, int x, float y, Paint paintAbove, Paint paintBelow) {
        int topY = baseline - (int) (y * baseline / waveformData.maxAmplitude);
        int bottomY = baseline + (int) (y * (getHeight() - baseline) / waveformData.maxAmplitude) - spaceWidth;
        canvas.drawRect(x, topY, x + barWidth, baseline, paintAbove);
        canvas.drawRect(x, baseline + spaceWidth, x + barWidth, bottomY, paintBelow);
    }
}
