package com.soundcloud.android.playback.ui.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class ProgressLineDrawable extends Drawable {

    private final Paint paint;
    private final Paint unPlayablePaint;
    private final int baseline;
    private final int thickness;
    private final float playableProportion;

    public ProgressLineDrawable(int color,
                                Paint unPlayablePaint,
                                int baseline,
                                int thickness,
                                float playableProportion) {
        this.baseline = baseline;
        this.thickness = thickness;
        this.playableProportion = playableProportion;
        this.paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        this.unPlayablePaint = unPlayablePaint;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(getBounds().left,
                        baseline,
                        getBounds().right * playableProportion,
                        baseline + thickness,
                        paint);
        if (playableProportion < 1) {
            canvas.drawRect(getBounds().right * playableProportion,
                            baseline,
                            getBounds().right,
                            baseline + thickness,
                            unPlayablePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
