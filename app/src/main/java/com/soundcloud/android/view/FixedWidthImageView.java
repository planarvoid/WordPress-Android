package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class FixedWidthImageView extends View {

    private int width;

    public FixedWidthImageView(Context context) {
        super(context);
    }

    public FixedWidthImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedWidthImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }
}
