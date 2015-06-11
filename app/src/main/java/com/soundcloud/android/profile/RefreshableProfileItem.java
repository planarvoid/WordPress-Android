package com.soundcloud.android.profile;

import com.soundcloud.android.view.MultiSwipeRefreshLayout;

public interface RefreshableProfileItem {
    void attachRefreshLayout(MultiSwipeRefreshLayout refreshLayout);
    void detachRefreshLayout();
}
