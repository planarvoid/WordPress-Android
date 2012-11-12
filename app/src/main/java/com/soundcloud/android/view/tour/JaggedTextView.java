package com.soundcloud.android.view.tour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.util.AttributeSet;
import com.soundcloud.android.view.CustomFontTextView;

public class JaggedTextView extends CustomFontTextView {
    private Paint mBackgroundPaint;

    public JaggedTextView(Context context) {
        super(context);
    }

    public JaggedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JaggedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Paint getBackgroundPaint() {
        if (mBackgroundPaint == null) {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(0x00000000);
        }

        return mBackgroundPaint;
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        if (drawable instanceof ColorDrawable) {
            getBackgroundPaint().setColor(((ColorDrawable) drawable).getColor());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getLayout() == null) {
            forceLayout();
        }

        Layout layout = getLayout();

        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (int line = 0; line < getLayout().getLineCount(); line++) {
            float left   = layout.getLineLeft(line);
            float top    = layout.getLineTop(line);
            float right  = layout.getLineRight(line);
            float bottom = layout.getLineBottom(line);

            // Apply padding to background rectangles
            if (line == 0) {
                top -= getPaddingTop();
            }

            if (line == layout.getLineCount() - 1) {
                bottom += getPaddingBottom();
            }

            left  -= getPaddingLeft();
            right += getPaddingRight();

            canvas.drawRect(left, top, right, bottom, getBackgroundPaint());
        }

        layout.getPaint().setColor(getTextColors().getDefaultColor());
        layout.draw(canvas);
    }
}
