package com.soundcloud.android.presentation;

import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.view.View;

public interface RefreshableScreen {

    MultiSwipeRefreshLayout getRefreshLayout();

    View[] getRefreshableViews();

}
