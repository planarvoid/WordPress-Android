package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SquareFrameLayout extends FrameLayout {
    public static final int MEASUREMENT_WIDTH = 0;
    public static final int MEASUREMENT_HEIGHT = 1;

    private static final int DEFAULT_DOMINANT_MEASUREMENT = MEASUREMENT_WIDTH;

    private final int dominantMeasurement;

    public SquareFrameLayout(Context context) {
        this(context, null);
    }

    public SquareFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SquareFrameLayout);
        dominantMeasurement = a.getInt(R.styleable.SquareFrameLayout_sfl_dominantMeasurement,
                DEFAULT_DOMINANT_MEASUREMENT);
        a.recycle();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        switch (dominantMeasurement) {
            case MEASUREMENT_WIDTH:
                super.onMeasure(widthMeasureSpec, widthMeasureSpec);
                break;

            case MEASUREMENT_HEIGHT:
                super.onMeasure(heightMeasureSpec, heightMeasureSpec);
                break;

            default:
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }
}
