package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;

/**
 * Wrapper around the retarded completely non-testable ActionBar-PullToRefresh APIs.
 * As we cannot write tests for this guy, keep its methods as simple as possible so that they're
 * "obviously correct".
 */
public class PullToRefreshWrapper {

    private MultiSwipeRefreshLayout swipeRefreshLayout;

    @Inject
    public PullToRefreshWrapper() {
        // For Dagger.
    }

    public void attach(MultiSwipeRefreshLayout pullToRefreshLayout, SwipeRefreshLayout.OnRefreshListener listener, int[] swipeToRefreshViewIds) {
        attachInternal(pullToRefreshLayout, listener);
        swipeRefreshLayout.setSwipeableChildren(swipeToRefreshViewIds);
    }

    private void attachInternal(MultiSwipeRefreshLayout pullToRefreshLayout, SwipeRefreshLayout.OnRefreshListener listener) {
        this.swipeRefreshLayout = pullToRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.setColorSchemeResources(R.color.sc_orange);
    }

    public void attach(MultiSwipeRefreshLayout pullToRefreshLayout, SwipeRefreshLayout.OnRefreshListener listener, View[] swipeToRefreshViews) {
        attachInternal(pullToRefreshLayout, listener);
        swipeRefreshLayout.setSwipeableChildren(swipeToRefreshViews);
    }

    public void detach() {
        this.swipeRefreshLayout = null;
    }

    // make this private or remove once we remove PullToRefreshController
    public boolean isAttached() {
        return swipeRefreshLayout != null;
    }

    public boolean isRefreshing() {
        return isAttached() && swipeRefreshLayout.isRefreshing();
    }

    public void setRefreshing(boolean refreshing) {
        if (isAttached()) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }
    }
}
