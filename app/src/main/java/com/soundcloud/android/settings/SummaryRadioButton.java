package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;

public class SummaryRadioButton extends AppCompatRadioButton {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String summary = Strings.EMPTY;

    public SummaryRadioButton(Context context) {
        this(context, null);
    }

    public SummaryRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(attrs);
    }

    private void initAttributes(AttributeSet attrs) {
        final int defaultSummarySize = getResources().getDimensionPixelSize(R.dimen.summary_radio_button_summary_size);
        final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SummaryRadioButton, 0, 0);
        paint.setColor(a.getColor(R.styleable.SummaryRadioButton_summaryColor, Color.BLACK));
        paint.setTextSize(a.getDimensionPixelSize(R.styleable.SummaryRadioButton_summarySize, defaultSummarySize));
        summary = Strings.nullToEmpty(a.getString(R.styleable.SummaryRadioButton_android_summary));
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int ascent = (int) -paint.ascent();
        final int descent = (int) paint.descent();
        final int newHeight = getBaseline() + getLineHeight() + ascent + descent;
        setMeasuredDimension(getMeasuredWidth(), newHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int descent = (int) paint.descent();
        final int x = getCompoundPaddingLeft() + getCompoundDrawablePadding();
        final int y = getMeasuredHeight() - descent;
        canvas.drawText(summary, x, y, paint);
    }

    public void setSummary(String summary) {
        this.summary = Strings.nullToEmpty(summary);
        invalidate();
    }
}
