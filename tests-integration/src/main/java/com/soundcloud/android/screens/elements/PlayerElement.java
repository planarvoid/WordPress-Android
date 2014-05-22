package com.soundcloud.android.screens.elements;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;
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
        solo.clickOnView(getCurrentTrackPage().findViewById(R.id.footer_controls));
        waiter.waitForExpandedPlayer();
    }

    public void pressBackToCollapse() {
        solo.goBack();
        waiter.waitForCollapsedPlayer();
    }

    public void pressCloseButton() {
        solo.clickOnView(getViewPager().findViewById(R.id.player_close));
    }

    public void tapNext() {
        solo.clickOnView(R.id.player_next);
    }

    public void tapPrevious() {
        solo.clickOnView(R.id.player_previous);
    }

    public void swipeNext() {
        solo.swipeLeft(.7f);
    }

    public void swipePrevious() {
        solo.swipeRight(.7f);
    }

    public String getTrackTitle() {
        View trackTitle = getCurrentTrackPage().findViewById(R.id.track_page_title);
        return ((TextView) trackTitle).getText().toString();
    }

    public void waitForContent() {
        waiter.waitForContent(getViewPager());
    }

    private ViewPager getViewPager() {
        return (ViewPager) solo.getView(R.id.player_track_pager);
    }

    private View getCurrentTrackPage() {
        ViewPagerElement viewPager = new ViewPagerElement(solo, R.id.player_track_pager);
        return viewPager.getCurrentPage(View.class);
    }

    public void toggleFooterPlay() {
        solo.clickOnView(R.id.footer_toggle);
    }

    public void togglePlay() {
        solo.clickOnView(R.id.track_page_artwork);
    }

    public boolean isFooterInPlayingState() {
        ToggleButton toggle = (ToggleButton) solo.getView(R.id.footer_toggle);
        return toggle.isChecked();
    }

    public boolean isPlayControlsVisible() {
        return solo.getView(R.id.player_play).getVisibility() == View.VISIBLE;
    }

    private SlidingUpPanelLayout getSlidingPanel() {
        return (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);
    }

}
