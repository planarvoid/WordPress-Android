package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

import javax.annotation.Nullable;

public class ListenableHorizontalScrollView extends HorizontalScrollView {

    @Nullable
    private OnScrollListener listener;

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

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener != null) {
            listener.onScroll(l, oldl);
        }
    }
}
