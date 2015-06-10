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
public class SwipeRefreshAttacher {

    private MultiSwipeRefreshLayout swipeRefreshLayout;

    @Inject
    public SwipeRefreshAttacher() {
        // For Dagger.
    }

    public void attach(SwipeRefreshLayout.OnRefreshListener listener, MultiSwipeRefreshLayout swipeRefreshLayout,
                       View... refreshableChildren) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(listener);
        swipeRefreshLayout.setSwipeableChildren(refreshableChildren);
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
