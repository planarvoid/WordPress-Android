package com.soundcloud.android.screens;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

public class PlayerScreen extends Screen {

    private static final Class ACTIVITY = MainActivity.class;

    public PlayerScreen(Han solo) {
        super(solo);
    }

    public boolean isExpanded() {
        waiter.waitForExpandedPlayer();
        return getSlidingPanel().isExpanded();
    }

    public boolean isCollapsed() {
        waiter.waitForCollapsedPlayer();
        return !getSlidingPanel().isExpanded();
    }

    public void tapFooter() {
        solo.clickOnView(R.id.footer_control);
        waiter.waitForExpandedPlayer();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private SlidingUpPanelLayout getSlidingPanel() {
        return (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);
    }

}
