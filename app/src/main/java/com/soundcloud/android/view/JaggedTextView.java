package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.util.AttributeSet;

public class JaggedTextView extends CustomFontTextView {

    private Paint backgroundPaint;
    private ColorStateList colorStateList;

    public JaggedTextView(Context context) {
        super(context);
    }

    public JaggedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs);
    }

    public JaggedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttributes(context, attrs);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.JaggedTextView);
        colorStateList = array.getColorStateList(R.styleable.JaggedTextView_jagged_background);
        array.recycle();

        backgroundPaint = new Paint();
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getLayout() == null) {
            forceLayout();
        }

        Layout layout = getLayout();
        if (layout == null) {
            return; // Still no reference to layout, so we can't draw the background on this pass
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());
        for (int line = 0; line < layout.getLineCount(); line++) {
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

            int backgroundColor = colorStateList.getColorForState(getDrawableState(), colorStateList.getDefaultColor());
            backgroundPaint.setColor(backgroundColor);
            canvas.drawRect(left, top, right, bottom, backgroundPaint);
        }

        layout.getPaint().setColor(getCurrentTextColor());
        layout.draw(canvas);
    }
}
