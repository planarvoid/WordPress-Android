package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.support.v4.widget.SwipeRefreshLayout;

import javax.inject.Inject;

/**
 * Wrapper around the retarded completely non-testable ActionBar-PullToRefresh APIs.
 * As we cannot write tests for this guy, keep its methods as simple as possible so that they're
 * "obviously correct".
 */
class PullToRefreshWrapper {

    private MultiSwipeRefreshLayout swipeRefreshLayout;

    @Inject
    public PullToRefreshWrapper() {
        // For Dagger.
    }

    public void attach(MultiSwipeRefreshLayout pullToRefreshLayout, SwipeRefreshLayout.OnRefreshListener listener) {
        this.swipeRefreshLayout = pullToRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.setSwipeableChildren(android.R.id.list, android.R.id.empty);
        swipeRefreshLayout.setColorSchemeResources(R.color.sc_orange);
    }

    void detach() {
        this.swipeRefreshLayout = null;
    }

    boolean isAttached() {
        return swipeRefreshLayout != null;
    }

    boolean isRefreshing() {
        return swipeRefreshLayout.isRefreshing();
    }

    void setRefreshing(boolean refreshing) {
        swipeRefreshLayout.setRefreshing(refreshing);
    }
}
