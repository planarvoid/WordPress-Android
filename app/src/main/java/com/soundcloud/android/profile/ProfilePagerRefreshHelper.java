package com.soundcloud.android.profile;

import com.soundcloud.android.Consts;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;

import android.util.SparseArray;

import javax.inject.Inject;

public class ProfilePagerRefreshHelper {

    private final MultiSwipeRefreshLayout refreshLayout;
    private final SparseArray<RefreshAware> refreshables = new SparseArray<>(ProfilePagerAdapter.FRAGMENT_COUNT);
    private int pendingRefreshPosition = Consts.NOT_SET;

    ProfilePagerRefreshHelper(MultiSwipeRefreshLayout refreshLayout) {
        this.refreshLayout = refreshLayout;
    }

    public void addRefreshable(int position, RefreshAware refreshable){
        refreshables.put(position, refreshable);
        if (position == pendingRefreshPosition) {
            refreshable.attachRefreshLayout(refreshLayout);
        }
    }

    public void setRefreshablePage(int position) {
        for (int i = 0; i < ProfilePagerAdapter.FRAGMENT_COUNT; i++) {
            if (i != position){
                final RefreshAware refreshable = refreshables.get(i);
                if (refreshable != null) {
                    refreshable.detachRefreshLayout();
                }
            }
        }

        final RefreshAware refreshable = refreshables.get(position);
        if (refreshable != null) {
            refreshable.attachRefreshLayout(refreshLayout);
            pendingRefreshPosition = Consts.NOT_SET;
        } else {
            pendingRefreshPosition = position;
        }
    }

    public void removeFragment(int position) {
        final RefreshAware fragment = refreshables.get(position);
        if (fragment != null) {
            fragment.detachRefreshLayout();
            refreshables.remove(position);
        }
    }

    public static class ProfilePagerRefreshHelperFactory {

        @Inject
        public ProfilePagerRefreshHelperFactory() {
            // dagger
        }

        public ProfilePagerRefreshHelper create(MultiSwipeRefreshLayout refreshLayout){
            return new ProfilePagerRefreshHelper(refreshLayout);
        }
    }
}