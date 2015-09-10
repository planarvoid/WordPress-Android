package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.Button;

public class RoundedColorButton extends Button {

    private static final int ROUNDING_DP = 4;

    private RectF rectangle;
    private Paint backgroundPaint;
    private ColorStateList backgroundColorStateList;

    private float roundingPx;

    public RoundedColorButton(Context context) {
        super(context);
    }

    public RoundedColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundedColorButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        roundingPx = ViewUtils.dpToPx(getContext(), ROUNDING_DP);
        rectangle = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
        setCustomAttributes(context, attrs);
    }

    public void setBackground(ColorStateList backgroundColorStateList) {
        this.backgroundColorStateList = backgroundColorStateList;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (backgroundColorStateList != null) {
            drawBackground(canvas);
        }
        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas) {
        int backgroundColor = backgroundColorStateList.getColorForState(getDrawableState(),
                backgroundColorStateList.getDefaultColor());
        backgroundPaint.setColor(backgroundColor);
        rectangle.right = getWidth();
        rectangle.bottom = getHeight();
        canvas.drawRoundRect(rectangle, roundingPx, roundingPx, backgroundPaint);
    }

    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundedColorButton, 0, 0);

        try {
            backgroundColorStateList = a.getColorStateList(R.styleable.RoundedColorButton_backgroundColorStateList);
        } finally {
            a.recycle();
        }
    }
}
