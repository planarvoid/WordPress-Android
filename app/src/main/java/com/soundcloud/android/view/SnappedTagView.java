package com.soundcloud.android.view;

import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;

public class SnappedTagView extends CustomFontTextView {

    private static final int GRID_SIZE_DP = 8;
    private static final int MAX_WIDTH_PADDING_DP = 8;

    public SnappedTagView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec) - ViewUtils.dpToPx(getContext(), MAX_WIDTH_PADDING_DP);
        setMeasuredDimension(roundUpWidth(getMeasuredWidth(), parentWidth), getMeasuredHeight());
    }

    @VisibleForTesting
    int roundUpWidth(int measuredWidth, int parentWidth) {
        final int gridSizePx = ViewUtils.dpToPx(getContext(), GRID_SIZE_DP);
        int targetWidth = measuredWidth + (gridSizePx - (measuredWidth % gridSizePx));
        return Math.min(parentWidth, targetWidth);
    }

}
