package com.soundcloud.android.screens;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

import android.widget.ToggleButton;

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

    public void toggleFooterPlay() {
        solo.clickOnView(R.id.footer_toggle);
    }

    public boolean isFooterInPlayingState() {
        ToggleButton toggle = (ToggleButton) solo.getView(R.id.footer_toggle);
        return toggle.isChecked();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    private SlidingUpPanelLayout getSlidingPanel() {
        return (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);
    }

}
