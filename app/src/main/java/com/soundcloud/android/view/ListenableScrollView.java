package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import javax.annotation.Nullable;

public class ListenableScrollView extends ScrollView {

    public interface OnScrollListener {
        void onScroll(int top, int oldTop);
    }

    @Nullable
    private OnScrollListener mListener;

    public ListenableScrollView(Context context) {
        super(context);
    }

    public ListenableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListenableScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        mListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mListener != null) {
            mListener.onScroll(t, oldt);
        }
    }
}
