package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.utils.ScrollHelper;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import javax.inject.Inject;

class PlaylistHeaderScrollHelper extends DefaultSupportFragmentLightCycle<Fragment> {
    private Optional<ScrollHelper> scrollHelper;

    @Inject
    public PlaylistHeaderScrollHelper() { }

    @Override
    public void onViewCreated(final Fragment fragment, View view, Bundle savedInstanceState) {
        if (view.findViewById(R.id.appbar) != null) {
            scrollHelper = Optional.of(new ScrollHelper(new PlaylistScrollScreen(fragment)));
        } else {
            scrollHelper = Optional.absent();
        }
    }

    @Override
    public void onStart(Fragment fragment) {
        if (scrollHelper.isPresent()) {
            scrollHelper.get().attach();
        }
    }

    @Override
    public void onStop(Fragment fragment) {
        if (scrollHelper.isPresent()) {
            scrollHelper.get().detach();
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        scrollHelper = null;
    }

    static class PlaylistScrollScreen implements ScrollHelper.ScrollScreen {
        private final MultiSwipeRefreshLayout refreshLayout;
        private final Toolbar toolBar;
        private final View contentView;
        private final View headerView;
        private final AppBarLayout appBarLayout;
        private final float toolbarElevationTarget;
        private final View emptyView;


        public PlaylistScrollScreen(Fragment fragment) {
            final FragmentActivity activity = fragment.getActivity();
            this.refreshLayout = ((RefreshableScreen) fragment).getRefreshLayout();
            toolBar = (Toolbar) activity.findViewById(R.id.toolbar_id);
            contentView = activity.findViewById(R.id.ak_recycler_view);
            headerView = activity.findViewById(R.id.playlist_details);
            appBarLayout = (AppBarLayout) activity.findViewById(R.id.appbar);
            toolbarElevationTarget = activity.getResources().getDimension(R.dimen.toolbar_elevation);
            emptyView = activity.findViewById(android.R.id.empty);
        }

        @Override
        public void setEmptyViewHeight(int height) {
            emptyView.getLayoutParams().height = height;
            emptyView.requestLayout();
        }

        @Override
        public void setSwipeToRefreshEnabled(boolean enabled) {
            refreshLayout.setEnabled(enabled);
        }

        @Override
        public AppBarLayout getAppBarLayout() {
            return appBarLayout;
        }

        @Override
        public View getHeaderView() {
            return headerView;
        }

        @Override
        public View getContentView() {
            return contentView;
        }

        @Override
        public Toolbar getToolbar() {
            return toolBar;
        }

        @Override
        public float getElevationTarget() {
            return toolbarElevationTarget;
        }
    }
}
