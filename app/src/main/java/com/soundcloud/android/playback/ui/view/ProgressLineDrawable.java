package com.soundcloud.android.playback.ui.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class ProgressLineDrawable extends Drawable {

    private final Paint paint;
    private final int baseline;
    private final int thickness;

    public ProgressLineDrawable(int color, int baseline, int thickness) {
        this.baseline = baseline;
        this.thickness = thickness;
        paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds().left, baseline, getBounds().right, baseline + thickness, paint);
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
