package com.soundcloud.android.presentation;

import com.soundcloud.android.R;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.support.v4.widget.SwipeRefreshLayout;

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
        this.swipeRefreshLayout = pullToRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.setSwipeableChildren(swipeToRefreshViewIds);
        swipeRefreshLayout.setColorSchemeResources(R.color.sc_orange);
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
