package com.soundcloud.android.stream;

import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;


public class StreamSwipeRefreshAttacher extends SwipeRefreshAttacher {
    private SwipeRefreshLayout.OnRefreshListener listener;

    @Inject
    StreamSwipeRefreshAttacher() {

    }

    @Override
    public void attach(SwipeRefreshLayout.OnRefreshListener listener,
                       MultiSwipeRefreshLayout swipeRefreshLayout,
                       View... refreshableChildren) {
        super.attach(listener, swipeRefreshLayout, refreshableChildren);

        this.listener = listener;
    }

    void forceRefresh() {
        if (listener != null) {
            listener.onRefresh();
        }
    }
}
