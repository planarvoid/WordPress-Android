package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;

public class VisualPlayerElement extends Element implements PlayerElement {

    public VisualPlayerElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return R.id.player_layout;
    }

    public ViewElement previousButton() {
        return solo.findElement(With.id(R.id.player_previous));
    }

    public ViewElement previousPageArea() {
        return solo.findElement(With.id(R.id.track_page_previous));
    }

    public ViewElement playButton() {
        return solo.findElement(With.id(R.id.player_play));
    }

    public ViewElement nextButton() {
        return solo.findElement(With.id(R.id.player_next));
    }

    public ViewElement nextPageArea(){
        return solo.findElement(With.id(R.id.track_page_next));
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
        return !footerPlayer().isVisible();
    }

    public boolean isCollapsed() {
        return footerPlayer().isVisible();
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

    @Override
    public void swipeNext() {
        solo.swipeLeft(0.4f);
        waiter.waitForPlayerPage();
    }

    public void swipePrevious() {
        solo.swipeRight(0.4f);
        waiter.waitForPlayerPage();
    }

    @Override
    public String getTrackTitle() {
        return trackTitle().getText();
    }

    public void waitForContent() {
        waiter.waitForContent(getViewPager());
    }

    private ViewPager getViewPager() {
        return solo.findElement(With.id(R.id.player_track_pager)).toViewPager();
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

    @Override
    public boolean isVisible() {
        return playerContainer().isVisible();
    }
}
