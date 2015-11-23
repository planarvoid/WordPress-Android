package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class WaveformScrollView extends HorizontalScrollView {

    @Nullable private OnScrollListener listener;

    private int areaWidth = Consts.NOT_SET;
    private final Rect scrubViewBounds = new Rect();

    public interface OnScrollListener {
        void onScroll(int left, int oldLeft);
        void onPress();
        void onRelease();
    }

    public WaveformScrollView(Context context) {
        super(context);
    }

    public WaveformScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WaveformScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setListener(OnScrollListener listener) {
        this.listener = listener;
    }

    public void setAreaWidth(int areaWidth) {
        this.areaWidth = areaWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener != null){
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                listener.onPress();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || isOutsideBounds(event)) {
                listener.onRelease();
            }
        }
        return super.onTouchEvent(event);
    }

    private boolean isOutsideBounds(MotionEvent event) {
        scrubViewBounds.set(getLeft(), getTop(), getRight(), getBottom());
        return !scrubViewBounds.contains(getLeft() + (int) event.getX(), getTop() + (int) event.getY());
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener != null) {
            listener.onScroll(l, oldl);
        }
    }

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        if (listener != null) {
            final int scrollX = getScrollX();
            listener.onScroll((int) (scrollX - getTranslationX()), scrollX);
        }
    }

    @Override
    public void fling(int velocityX) {
        int newVelocityX = areaWidth == Consts.NOT_SET
                ? velocityX
                : (int) (velocityX / ((float) areaWidth / getWidth()));

        super.fling(newVelocityX);
    }
}
