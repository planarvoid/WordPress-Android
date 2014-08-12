package com.soundcloud.android.playback.ui.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.Button;

public class LearnMoreButton extends Button {

    private RectF rectangle;
    private Paint backgroundPaint;
    private ColorStateList backgroundColorStateList;

    public LearnMoreButton(Context context) {
        super(context);
    }

    public LearnMoreButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes();
    }

    public LearnMoreButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectangle = new RectF(0.0f, 0.0f, (float) getWidth(), (float) getHeight());
    }

    private void initAttributes() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
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
        canvas.drawRoundRect(rectangle, 8.0f, 8.0f, backgroundPaint);
    }

}
