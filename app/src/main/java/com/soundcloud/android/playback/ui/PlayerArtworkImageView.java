package com.soundcloud.android.playback.ui;

import com.soundcloud.android.view.AspectRatioImageView;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;


/**
 * Offsets the imageview source by 20% from the top
 * Used by {@link com.soundcloud.android.explore.ExploreTracksAdapter}
 */
public class PlayerArtworkImageView extends AspectRatioImageView {

    private float progress;
    private float lastDx;

    public PlayerArtworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }


    public void setProgressProportion(float progress) {
        this.progress = progress;
        setImageMatrix(getWidth(), getHeight());
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        setImageMatrix(r - l, b - t);
        return super.setFrame(l, t, r, b);
    }

    private void setImageMatrix(int vwidth, int vheight) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {

            final int dwidth = drawable.getIntrinsicWidth();
            final int dheight = drawable.getIntrinsicHeight();

            float scale;
            float dx = 0, dy = 0;
            final Matrix matrix = getImageMatrix();

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                final int excessWidth = (int) (dwidth * scale - vwidth);
                final float offset = (-excessWidth / 2 + excessWidth * progress);
                dx = (vwidth - dwidth * scale) * 0.5f - offset;
                // optimisation. Do not redraw unnecessarily
                if (dx != lastDx) {
                    lastDx = dx;
                    invalidate();
                }
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            setImageMatrix(matrix);
        }
    }
}