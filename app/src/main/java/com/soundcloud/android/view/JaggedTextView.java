package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;

public class JaggedTextView extends CustomFontTextView {

    private Paint backgroundPaint;
    private ColorStateList colorStateList;

    private boolean showBackground = true;

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
        setIncludeFontPadding(false);
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        // Does not support background drawable
    }

    @Override
    public void setBackground(Drawable background) {
        // Does not support background drawable
    }

    @VisibleForTesting
    public boolean isShowingBackground() {
        return showBackground;
    }

    public void showBackground(boolean showBackground) {
        this.showBackground = showBackground;
        invalidate();
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

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        if (showBackground && !TextUtils.isEmpty(getText())) {
            drawBackground(canvas, layout);
        }
        canvas.restore();

        super.onDraw(canvas);
    }

    private void drawBackground(Canvas canvas, Layout layout) {
        for (int line = 0; line < layout.getLineCount(); line++) {
            final int compoundPaddingTop = getCompoundPaddingTop();
            final int compoundPaddingBottom = getCompoundPaddingBottom();

            float left = layout.getLineLeft(line);
            float top = layout.getLineTop(line);
            float right = layout.getLineRight(line) + getDrawablePaddingLeft() + getDrawablePaddingRight();
            float bottom = layout.getLineBottom(line);

            // Apply padding to background rectangles
            if (line == 0) {
                top -= compoundPaddingTop;
            }

            if (shouldAddExtraPadding(layout, line)) {
                bottom += compoundPaddingBottom;
            }

            left -= getPaddingLeft();
            right += getPaddingRight();

            int backgroundColor = colorStateList.getColorForState(getDrawableState(), colorStateList.getDefaultColor());
            backgroundPaint.setColor(backgroundColor);
            canvas.drawRect(left, top, right, bottom, backgroundPaint);
        }
    }

    private boolean shouldAddExtraPadding(Layout layout, int line) {
        final boolean isAndroidVersionMissingExtraSpacingAfterLastLine = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        return isAndroidVersionMissingExtraSpacingAfterLastLine && line == layout.getLineCount() - 1;
    }

    private int getDrawablePaddingLeft() {
        return getCompoundPaddingLeft() - getPaddingLeft();
    }

    private int getDrawablePaddingRight() {
        return getCompoundPaddingRight() - getPaddingRight();
    }
}
