package com.soundcloud.android.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class NewPlaylistDetailHeaderScrollHelper extends DefaultSupportFragmentLightCycle<Fragment> implements AppBarLayout.OnOffsetChangedListener {

    @BindView(R.id.str_layout) MultiSwipeRefreshLayout swipeRefreshLayout;

    private boolean atTop = true;
    private boolean isEditing;

    @Inject
    NewPlaylistDetailHeaderScrollHelper() {
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        atTop = verticalOffset == 0;
        configureSwipeToRefresh();
    }

    public void setIsEditing(boolean isEditing){
        this.isEditing = isEditing;
        configureSwipeToRefresh();
    }

    private void configureSwipeToRefresh() {
        boolean shouldBeEnabled = atTop && !isEditing;

        // do not remove this check. Setting the same value twice is not a no-op and causes animation problems
        if (swipeRefreshLayout.isEnabled() != shouldBeEnabled) {
            swipeRefreshLayout.setEnabled(shouldBeEnabled);
        }
    }
}
