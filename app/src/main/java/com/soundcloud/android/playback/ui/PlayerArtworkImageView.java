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
    protected boolean setFrame(int left, int top, int right, int bottom) {
        setImageMatrix(right - left, bottom - top);
        return super.setFrame(left, top, right, bottom);
    }

    private void setImageMatrix(int vWidth, int vHeight) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {

            final int dWidth = drawable.getIntrinsicWidth();
            final int dHeight = drawable.getIntrinsicHeight();

            float scale;
            float dX = 0, dY = 0;
            final Matrix matrix = getImageMatrix();

            if (dWidth * vHeight > vWidth * dHeight) {
                scale = (float) vHeight / (float) dHeight;
                final int excessWidth = (int) (dWidth * scale - vWidth);
                final float offset = -excessWidth / 2 + excessWidth * progress;
                dX = (vWidth - dWidth * scale) * 0.5f - offset;

                // Optimisation prevents unnecessary redraw in the same position
                if (Math.abs(dX - lastDx) > .0001) {
                    lastDx = dX;
                    invalidate();
                }
            } else {
                scale = (float) vWidth / (float) dWidth;
                dY = (vHeight - dHeight * scale) * 0.5f;
            }

            matrix.setScale(scale, scale);
            matrix.postTranslate(dX, dY);
            setImageMatrix(matrix);
        }
    }
}