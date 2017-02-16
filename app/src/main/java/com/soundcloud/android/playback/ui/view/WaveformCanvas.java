package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.waveform.WaveformData;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

public class WaveformCanvas extends TextureView implements TextureView.SurfaceTextureListener {
    private static final float MIN_UNPLAYABLE = 0.99f;

    private WaveformData waveformData;
    private Paint abovePaint;
    private Paint belowPaint;
    private Paint unplayableAbovePaint;
    private Paint unplayableBelowPaint;
    private int barWidth, spaceWidth, baseline;
    private boolean surfaceAvailable = false;
    private boolean drawn = false;
    private boolean visible = false;

    private float unplayableFromPosition = 1.0f;

    public WaveformCanvas(Context context) {
        this(context, null);
    }

    public WaveformCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(this);
        setOpaque(false);
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
        this.drawn = false;
        this.visible = false;
    }

    // Note: Progress events might have slightly less duration than metadata duration
    // but they should still be handled as fully-playable
    public void setUnplayableFromPosition(float unplayableFromPosition) {
        boolean isBelowFullPlayable = unplayableFromPosition < MIN_UNPLAYABLE;
        boolean wasBelowFullPlayable = this.unplayableFromPosition < MIN_UNPLAYABLE;

        if (isBelowFullPlayable != wasBelowFullPlayable) {
            this.unplayableFromPosition = unplayableFromPosition;
            this.drawn = false;
            drawCanvas();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceAvailable = true;
        drawCanvas();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        drawn = false;
        drawCanvas();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surfaceAvailable = false;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void show() {
        visible = true;
        drawCanvas();
    }

    public void drawCanvas() {
        if (!surfaceAvailable || drawn || !visible) {
            return;
        }

        Canvas canvas = lockCanvas();
        if (canvas == null) {
            return;
        }

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

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

            drawn = true;
        }

        unlockCanvasAndPost(canvas);
    }

    private void drawBar(Canvas canvas, int x, float y, Paint paintAbove, Paint paintBelow) {
        int topY = baseline - (int) (y * baseline / waveformData.maxAmplitude);
        int bottomY = baseline + (int) (y * (getHeight() - baseline) / waveformData.maxAmplitude) - spaceWidth;
        canvas.drawRect(x, topY, x + barWidth, baseline, paintAbove);
        canvas.drawRect(x, baseline + spaceWidth, x + barWidth, bottomY, paintBelow);
    }
}
