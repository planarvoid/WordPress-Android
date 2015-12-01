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
    private WaveformData waveformData;
    private Paint abovePaint;
    private Paint belowPaint;
    private Paint unplayableAbovePaint;
    private Paint unplayableBelowPaint;
    private int barWidth, spaceWidth, baseline;
    private boolean surfaceAvailable = false;

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

    public void initialize(WaveformData waveformData, Paint abovePaint, Paint belowPaint, Paint unplayableAbovePaint, Paint unplayableBelowPaint, int barWidth, int spaceWidth, int baseline) {
        this.waveformData = waveformData;
        this.abovePaint = abovePaint;
        this.belowPaint = belowPaint;
        this.barWidth = barWidth;
        this.spaceWidth = spaceWidth;
        this.baseline = baseline;
        this.unplayableAbovePaint = unplayableAbovePaint;
        this.unplayableBelowPaint = unplayableBelowPaint;
        drawCanvas();
    }

    public void setUnplayableFromPosition(float unplayableFromPosition) {
        this.unplayableFromPosition = unplayableFromPosition;
        drawCanvas();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surfaceAvailable = true;
        drawCanvas();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
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

    public void drawCanvas() {
        if (!surfaceAvailable) {
            return;
        }

        Canvas canvas = lockCanvas();
        if (canvas == null) {
            return;
        }

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        if (this.waveformData != null) {
            int w = barWidth + spaceWidth;
            int x = 0;
            float y;

            final int length = waveformData.samples.length;
            final int playableBars = (int) (length * unplayableFromPosition);
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

        unlockCanvasAndPost(canvas);
    }

    private void drawBar(Canvas canvas, int x, float y, Paint paintAbove, Paint paintBelow) {
        int topY = baseline - (int) (y * baseline / waveformData.maxAmplitude);
        int bottomY = baseline + (int) (y * (getHeight() - baseline) / waveformData.maxAmplitude) - spaceWidth;
        canvas.drawRect(x, topY, x + barWidth, baseline, paintAbove);
        canvas.drawRect(x, baseline + spaceWidth, x + barWidth, bottomY, paintBelow);
    }
}
