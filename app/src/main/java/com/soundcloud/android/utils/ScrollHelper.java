package com.soundcloud.android.utils;

import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ScrollHelper implements AppBarLayout.OnOffsetChangedListener {

    private final ScrollScreen screen;

    public interface ScrollScreen {

        void setEmptyViewHeight(int height);

        void setSwipeToRefreshEnabled(boolean enabled);

        AppBarLayout getAppBarLayout();

        View getHeaderView();

        View getContentView();

        Toolbar getToolbar();

        float getElevationTarget();
    }

    private static final int TOP = 0;
    private int offset = 0;

    public ScrollHelper(ScrollScreen screen) {
        this.screen = screen;
    }

    public void attach() {
        ViewCompat.setElevation(screen.getToolbar(), 0);
        screen.getAppBarLayout().addOnOffsetChangedListener(this);
    }

    public void detach() {
        screen.getAppBarLayout().removeOnOffsetChangedListener(this);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        this.offset = offset;
        updateElevation();
        setScreenOffset();
    }

    private void updateElevation() {
        ViewCompat.setTranslationZ(screen.getToolbar(), calculateElevation(offset));
    }

    private float calculateElevation(int offset) {
        float headerHeight = screen.getHeaderView().getHeight();
        return Math.min(screen.getElevationTarget(), (headerHeight / 2) - Math.abs((headerHeight / 2) + offset));
    }

    private void setScreenOffset() {
        screen.setEmptyViewHeight(calculateListHeight());
        screen.setSwipeToRefreshEnabled(offset >= TOP);
    }

    private int calculateListHeight() {
        return screen.getContentView().getHeight() - screen.getAppBarLayout().getTotalScrollRange() - offset;
    }

}
