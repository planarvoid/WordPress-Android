package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class CircularBorderImageView extends AppCompatImageView {

    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final int DEFAULT_BORDER_COLOR = Color.BLACK;

    private final RectF borderRect = new RectF();
    private final Paint borderPaint = new Paint();

    private int borderColor = DEFAULT_BORDER_COLOR;
    private int borderWidth = DEFAULT_BORDER_WIDTH;

    private float borderRadius;

    private boolean ready;
    private boolean setupPending;

    public CircularBorderImageView(Context context) {
        super(context);

        init();
    }

    public CircularBorderImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularBorderImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularBorderImageView, defStyle, 0);

        borderWidth = a.getDimensionPixelSize(R.styleable.CircularBorderImageView_cbiv_borderWidth,
                                              DEFAULT_BORDER_WIDTH);
        borderColor = a.getColor(R.styleable.CircularBorderImageView_cbiv_borderColor, DEFAULT_BORDER_COLOR);

        a.recycle();

        init();
    }

    private void init() {
        ready = true;
        if (setupPending) {
            setup();
            setupPending = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (borderWidth != 0) {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, borderRadius, borderPaint);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }

    private void setup() {
        if (!ready) {
            setupPending = true;
            return;
        }

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);

        borderRect.set(0, 0, getWidth(), getHeight());
        borderRadius = Math.min((borderRect.height() - borderWidth) / 2, (borderRect.width() - borderWidth) / 2);

        invalidate();
    }
}
