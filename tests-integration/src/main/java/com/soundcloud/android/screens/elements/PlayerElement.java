package com.soundcloud.android.screens.elements;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.widget.ToggleButton;

public class PlayerElement extends Element {

    public PlayerElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return R.id.player_layout;
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

    private SlidingUpPanelLayout getSlidingPanel() {
        return (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);
    }

}
