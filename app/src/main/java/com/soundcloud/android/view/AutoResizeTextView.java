package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;

/**
 * Based on:
 * http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds
 */
public class AutoResizeTextView extends CustomFontTextView {

    private static final int MIN_TEXT_SIZE = 16;
    private static final int RESIZE_STEP_TEXT_SIZE = 4;
    private static final String ELLIPSIS = "\u2026";

    private boolean needsResize = false;
    private float textSize;
    private float minTextSize;
    private float resizeStep;
    private float spacingMult = 1.0f;
    private float spacingAdd = 0.0f;
    private boolean addEllipsis = true;

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AutoResizeTextView);
        final float density = context.getResources().getDisplayMetrics().density;

        addEllipsis = array.getBoolean(R.styleable.AutoResizeTextView_addEllipsis, true);
        minTextSize = array.getDimensionPixelSize(R.styleable.AutoResizeTextView_minTextSize,
                                                  (int) (MIN_TEXT_SIZE * density));
        resizeStep = array.getDimensionPixelSize(R.styleable.AutoResizeTextView_resizeStep,
                                                 (int) (RESIZE_STEP_TEXT_SIZE * density));
        array.recycle();
        textSize = getTextSize();
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        needsResize = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            needsResize = true;
        }
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        textSize = getTextSize();
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        textSize = getTextSize();
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        spacingMult = mult;
        spacingAdd = add;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed || needsResize) {
            int widthLimit = (right - left) - getCompoundPaddingLeft() - getCompoundPaddingRight();
            int heightLimit = (bottom - top) - getCompoundPaddingBottom() - getCompoundPaddingTop();
            resizeText(widthLimit, heightLimit);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    public void resizeText(int width, int height) {
        CharSequence text = getText();
        // Do not resize if the view does not have dimensions or there is no text
        if (text == null || text.length() == 0 || height <= 0 || width <= 0 || textSize == 0) {
            return;
        }

        if (getTransformationMethod() != null) {
            text = getTransformationMethod().getTransformation(text, this);
        }

        TextPaint textPaint = getPaint();
        int textHeight = getTextHeight(text, textPaint, width, textSize);

        // Until we either fit within our text view or we had reached our min text size, incrementally try smaller sizes
        while (textHeight > height && textSize > minTextSize) {
            textSize = Math.max(textSize - resizeStep, minTextSize);
            textHeight = getTextHeight(text, textPaint, width, textSize);
        }

        appendEllipsisWhenNeeded(width, height, text, textPaint, textHeight);

        // Some devices try to auto adjust line spacing, so force default line spacing
        // and invalidate the layout as a side effect
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        setLineSpacing(spacingAdd, spacingMult);

        // Reset force resize flag
        needsResize = false;
    }

    private void appendEllipsisWhenNeeded(int width,
                                          int height,
                                          CharSequence text,
                                          TextPaint textPaint,
                                          int textHeight) {
        if (addEllipsis && textSize == minTextSize && textHeight > height) {
            TextPaint paint = new TextPaint(textPaint);
            StaticLayout layout = new StaticLayout(text,
                                                   paint,
                                                   width,
                                                   Layout.Alignment.ALIGN_NORMAL,
                                                   spacingMult,
                                                   spacingAdd,
                                                   false);
            if (layout.getLineCount() > 0) {
                // Since the line at the specific vertical position would be cut off,
                // we must trim up to the previous line
                int lastLine = layout.getLineForVertical(height) - 1;
                // If the text would not even fit on a single line, clear it
                if (lastLine < 0) {
                    setText("");
                } else {
                    int start = layout.getLineStart(lastLine);
                    int end = layout.getLineEnd(lastLine);
                    float lineWidth = layout.getLineWidth(lastLine);
                    float ellipseWidth = textPaint.measureText(ELLIPSIS);

                    // Trim characters off until we have enough room to draw the ellipsis
                    while (width < lineWidth + ellipseWidth) {
                        lineWidth = textPaint.measureText(text.subSequence(start, --end + 1).toString());
                    }
                    final String textWithEllipsis = text.subSequence(0, end) + ELLIPSIS;
                    setText(textWithEllipsis);
                }
            }
        }
    }

    // Set the text size of the text paint object and use a static layout to render text off screen before measuring
    private int getTextHeight(CharSequence source, TextPaint paint, int width, float textSize) {
        // modified: make a copy of the original TextPaint object for measuring
        // (apparently the object gets modified while measuring, see also the
        // docs for TextView.getPaint() (which states to access it read-only)
        TextPaint paintCopy = new TextPaint(paint);
        paintCopy.setTextSize(textSize);
        StaticLayout layout = new StaticLayout(source,
                                               paintCopy,
                                               width,
                                               Layout.Alignment.ALIGN_NORMAL,
                                               spacingMult,
                                               spacingAdd,
                                               true);
        return layout.getHeight();
    }

}
