package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class DynamicSafeViewPager extends SafeViewPager {

    public DynamicSafeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicSafeViewPager(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height = Math.max(height, child.getMeasuredHeight());
        }

        if (height > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
