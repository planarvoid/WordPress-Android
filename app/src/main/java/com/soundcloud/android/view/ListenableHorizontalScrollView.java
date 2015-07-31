package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class ListenableHorizontalScrollView extends HorizontalScrollView {

    @Nullable
    private OnScrollListener listener;

    private int areaWidth = Consts.NOT_SET;

    public interface OnScrollListener {
        void onScroll(int left, int oldLeft);
    }

    public ListenableHorizontalScrollView(Context context) {
        super(context);
    }

    public ListenableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListenableHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.listener = listener;
    }

    public void setAreaWidth(int areaWidth) {
        this.areaWidth = areaWidth;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener != null) {
            listener.onScroll(l, oldl);
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
