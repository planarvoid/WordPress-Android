package com.soundcloud.android.profile;

import com.soundcloud.android.view.MultiSwipeRefreshLayout;

public interface RefreshAware {
    void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout);
    void detachRefreshLayout();
}
