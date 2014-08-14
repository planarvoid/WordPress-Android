package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
        init();
    }

    public RoundedColorButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        roundingPx = ViewUtils.dpToPx(getContext(), ROUNDING_DP);
        rectangle = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void setBackground(ColorStateList backgroundColorStateList) {
        this.backgroundColorStateList = backgroundColorStateList;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
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

}
