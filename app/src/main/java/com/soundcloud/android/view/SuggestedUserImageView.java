package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;

/**
 * Offsets the imageview source by ~16%
 * Used by {@link SuggestedUserItemLayout}, {@link com.soundcloud.android.adapter.SuggestedUsersAdapter}
 */
public class SuggestedUserImageView extends OptimisedImageView {

    public SuggestedUserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b)
    {
        Matrix matrix = getImageMatrix();
        float scaleFactor = getWidth()/(float)getDrawable().getIntrinsicWidth();
        matrix.setScale(scaleFactor, scaleFactor, 0, 0);
        matrix.postTranslate(0, -(b-t)/6);
        setImageMatrix(matrix);
        return super.setFrame(l, t, r, b);
    }
}
