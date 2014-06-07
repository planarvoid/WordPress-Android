package com.soundcloud.android.screens.elements;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;

public class PlayerElement extends Element {

    public PlayerElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return R.id.player_layout;
    }

    private ViewElement playerContainer() {
        return solo.findElement(With.id(R.id.player_root));
    }

    private ViewElement closeButton() {
        return solo.findElement(With.id(R.id.player_close));
    }

    private ViewElement artwork() {
        return solo.findElement(With.id(R.id.track_page_artwork));
    }

    private ViewElement previousButton() {
        return solo.findElement(With.id(R.id.player_previous));
    }

    private ViewElement previousPageArea() {
        //TODO: this is an invisible view, wonder how it's gonna work with new driver
        return solo.findElement(With.id(R.id.track_page_previous));
    }

    private ViewElement playButton() {
        return solo.findElement(With.id(R.id.player_play));
    }

    private ViewElement nextButton() {
        return solo.findElement(With.id(R.id.player_next));
    }

    private ViewElement nextPageArea(){
        //TODO: this is an invisible view, wonder how it's gonna work with new driver
        return solo.findElement(With.id(R.id.track_page_next));
    }

    private ViewElement footerPlayToggle() {
        return solo.findElement(With.id(R.id.footer_toggle));
    }

    private ViewElement trackTitle() {
        return solo.findElement(With.id(R.id.track_page_title));
    }

    private ViewElement footerPlayer() {
        return solo.findElement(With.id(R.id.footer_controls));
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
        footerPlayer().click();
        waiter.waitForExpandedPlayer();
    }

    public void pressBackToCollapse() {
        solo.goBack();
        waiter.waitForCollapsedPlayer();
    }

    public void pressCloseButton() {
        closeButton().click();
        waiter.waitForCollapsedPlayer();
    }

    public void tapNext() {
        nextButton().click();
        waiter.waitForPlayerPage();
    }

    public void tapPrevious() {
        previousButton().click();
        waiter.waitForPlayerPage();
    }

    public void tapTrackPageNext() {
        nextPageArea().click();
        waiter.waitForPlayerPage();
    }

    public void tapTrackPagePrevious() {
        previousPageArea().click();
        waiter.waitForPlayerPage();
    }

    public void swipeNext() {
        solo.swipeLeft(.7f);
        waiter.waitForPlayerPage();
    }

    public void swipePrevious() {
        solo.swipeRight(.7f);
        waiter.waitForPlayerPage();
    }

    public String getTrackTitle() {
        return trackTitle().getText();
    }

    public void waitForContent() {
        waiter.waitForContent(getViewPager());
    }

    private ViewPager getViewPager() {
        return (ViewPager) solo.getView(R.id.player_track_pager);
    }

    public void toggleFooterPlay() {
        footerPlayToggle().click();
    }

    public void clickArtwork() {
        artwork().click();
    }

    public boolean isFooterInPlayingState() {
        return footerPlayToggle().isChecked();
    }

    public boolean isPlayControlsVisible() {
        return playButton().isVisible();
    }

    private SlidingUpPanelLayout getSlidingPanel() {
        return (SlidingUpPanelLayout) solo.getView(R.id.sliding_layout);
    }

    public boolean isVisible() {
        return playerContainer().isVisible();
    }
}
