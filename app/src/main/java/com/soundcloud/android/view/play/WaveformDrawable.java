package com.soundcloud.android.view.play;

import com.soundcloud.android.model.WaveformData;
import org.jetbrains.annotations.NotNull;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

/**
 * Takes {@link WaveformData} and renders it.
 */
public class WaveformDrawable extends Drawable {
    private @NotNull final WaveformData mData;
    private final Paint mDrawPaint;

    public WaveformDrawable(@NotNull WaveformData data, int drawColor) {
        if (data == null) throw new IllegalArgumentException("Need waveform data");

        mData = data;
        mDrawPaint = new Paint();
        mDrawPaint.setColor(drawColor);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        final WaveformData scaled = mData.scale(canvas.getWidth());

        final int height = canvas.getHeight();

        for (int i =0; i<scaled.samples.length; i++) {
            final float scaledHeight = scaled.samples[i] * (float) height / scaled.maxAmplitude;
            canvas.drawLine(
                    i, 0,
                    i, height - scaledHeight
                    , mDrawPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 95;
    }
}
