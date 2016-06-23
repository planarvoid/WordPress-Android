package com.soundcloud.android.settings;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class UsageBarView extends View {

    private final List<UsageBar> bars = new ArrayList<>();
    private final Paint borderPaint = new Paint();
    private double totalAmount;

    public UsageBarView(Context context) {
        super(context);
        setColors();
    }

    public UsageBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColors();
    }

    public UsageBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setColors();
    }

    public UsageBarView addBar(int colorId, long amount) {
        bars.add(new UsageBar(getResources().getColor(colorId), amount));
        totalAmount += Math.max(0, amount);
        invalidate();
        return this;
    }

    public UsageBarView reset() {
        bars.clear();
        totalAmount = 0d;
        return this;
    }

    private void setColors() {
        borderPaint.setColor(getResources().getColor(R.color.usage_bar_border));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (totalAmount == 0) return;

        int width = getMeasuredWidth() - 2;
        int height = getMeasuredHeight() - 2;
        float x = 1f;
        float barWidth;

        canvas.drawRect(0, 0, width + 2, height + 2, borderPaint);

        for (UsageBar bar : bars) {
            barWidth = (float) (bar.getAmount() * width / totalAmount);
            canvas.drawRect(x, 1, x + barWidth, height + 1, bar.getPaint());
            x += barWidth;
        }
    }

    private class UsageBar {
        private Paint paint;
        private long amount;

        UsageBar(int color, long amount) {
            this.paint = new Paint();
            this.paint.setColor(color);
            this.amount = Math.max(0, amount);
        }

        public Paint getPaint() {
            return paint;
        }

        public long getAmount() {
            return amount;
        }
    }

}
