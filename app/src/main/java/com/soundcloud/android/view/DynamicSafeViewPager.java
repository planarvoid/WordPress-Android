package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class DynamicSafeViewPager extends SafeViewPager {

    private int lastHeight = 0;

    public DynamicSafeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicSafeViewPager(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (lastHeight == 0) {
            measureHeight(widthMeasureSpec);
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(lastHeight, MeasureSpec.EXACTLY));
    }

    // Note: This only works when the height of all items in the adapter are always the same.
    private void measureHeight(int widthMeasureSpec) {
        if (getAdapter().getCount() > 0) {
            View child = (View) getAdapter().instantiateItem(this, 0);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            lastHeight = child.getMeasuredHeight();
        }
    }

}
