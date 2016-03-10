package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ErrorUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EdgeEffect;
import android.widget.HorizontalScrollView;

import java.lang.reflect.Field;

public class WaveformScrollView extends HorizontalScrollView {

    private static final int BASE_OVERSCROLL_DISTANCE = 60;
    @Nullable private OnScrollListener listener;

    private int areaWidth = Consts.NOT_SET;
    private final Rect scrubViewBounds = new Rect();
    private int adjustedMaxOverScrollX;

    public interface OnScrollListener {
        void onScroll(int left, int oldLeft);
        void onPress();
        void onRelease();
    }

    public WaveformScrollView(Context context) {
        super(context);
        init(context);
    }

    public WaveformScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WaveformScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        adjustedMaxOverScrollX = (int) (BASE_OVERSCROLL_DISTANCE * context.getResources().getDisplayMetrics().density);
        killEdgeEffect(context);
    }

    private void killEdgeEffect(Context context) {
        try {
            Field edgeGlowLeft = HorizontalScrollView.class.getDeclaredField("mEdgeGlowLeft");
            edgeGlowLeft.setAccessible(true);
            edgeGlowLeft.set(this, new NoEdgeEffect(context));

            Field edgeGlowRight = HorizontalScrollView.class.getDeclaredField("mEdgeGlowRight");
            edgeGlowRight.setAccessible(true);
            edgeGlowRight.set(this, new NoEdgeEffect(context));
        } catch (Exception e) {
            ErrorUtils.handleSilentException("Unable to hide Edge Glow", e);
        }
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
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, adjustedMaxOverScrollX, maxOverScrollY, isTouchEvent);
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

    private static class NoEdgeEffect extends EdgeEffect
    {
        public NoEdgeEffect(Context context) {
            super(context);
        }
        public boolean draw(Canvas canvas) {
            // Do nothing
            return false;
        }
    }
}
