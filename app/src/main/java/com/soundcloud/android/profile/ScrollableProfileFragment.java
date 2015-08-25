package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.View;

public abstract class ScrollableProfileFragment extends LightCycleSupportFragment implements ProfileScreen, RefreshableScreen {

    private Integer pendingEmptyViewHeight;
    private Boolean pendingRefreshState;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (pendingEmptyViewHeight != null){
            setEmptyViewHeight(pendingEmptyViewHeight);
            pendingEmptyViewHeight = null;
        }

        if (pendingRefreshState != null) {
            setSwipeToRefreshEnabled(pendingRefreshState);
            pendingEmptyViewHeight = null;
        }
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public void setEmptyViewHeight(int height) {
        if (getView() != null) {
            final View emptyView = getView().findViewById(android.R.id.empty);
            emptyView.getLayoutParams().height = height;
            emptyView.requestLayout();
        } else {
            pendingEmptyViewHeight = height;
        }
    }

    @Override
    public void setSwipeToRefreshEnabled(boolean enabled) {
        if (getView() != null) {
            getRefreshLayout().setEnabled(enabled);
        } else {
            pendingRefreshState = enabled;
        }
    }
}
