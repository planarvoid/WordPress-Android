package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.support.v4.view.ViewPager;

public class VisualPlayerElement extends Element {

    private final With footerPlayerPredicate = With.id(R.id.footer_controls);

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

    public ViewElement skipAd() {return solo.findElement(With.id(R.id.skip_ad));};

    private ViewElement trackContainer() {
        return solo.findElement(With.id(R.id.player_track_pager));
    }

    private ViewElement closeButton() {
        return solo.findElement(With.id(R.id.player_close));
    }

    private ViewElement artwork() {
        return solo.findElement(With.id(R.id.track_page_artwork));
    }

    private ViewElement creator() {
        return solo.findElement(With.id(R.id.track_page_user));
    }

    private ViewElement footerPlayToggle() {
        return solo.findElement(With.id(R.id.footer_toggle));
    }

    private ViewElement trackTitle() {
        return solo.findElement(With.id(R.id.track_page_title));
    }

    private ViewElement footerPlayer() {
        return solo.findElement(footerPlayerPredicate);
    }

    public boolean isExpanded() {
        return !footerPlayer().isVisible();
    }

    public boolean isCollapsed() {
        return footerPlayer().isVisible();
    }

    public void tapFooter() {
        footerPlayer().click();
        waitForExpandedPlayer();
    }

    public void pressBackToCollapse() {
        solo.goBack();
        waitForCollapsedPlayer();
    }

    public void pressCloseButton() {
        closeButton().click();
        waitForCollapsedPlayer();
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

    public void tapSkipAd() {
        skipAd().click();
        waiter.waitForPlayerPage();
    }

    public void swipeNext() {
        solo.swipeLeft(0.4f);
        waiter.waitForPlayerPage();
    }

    public void swipePrevious() {
        solo.swipeRight(0.4f);
        waiter.waitForPlayerPage();
    }

    public String getTrackTitle() {
        return trackTitle().getText();
    }

    public String getTrackCreator() {
        return creator().getText();
    }

    public void waitForContent() {
        waiter.waitForContent(getViewPager());
    }

    public boolean waitForExpandedPlayer() {
        return waiter.waitForElementToBeInvisible(footerPlayerPredicate);
    }

    public boolean waitForCollapsedPlayer() {
        return waiter.waitForElementToBeVisible(footerPlayerPredicate);
    }

    public void waitForSkipAdButton() {
        waiter.waitForElement(R.id.skip_ad);
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

    public ProfileScreen clickCreator() {
        creator().click();
        return new ProfileScreen(solo);
    }

    public boolean isFooterInPlayingState() {
        return footerPlayToggle().isChecked();
    }

    public boolean isPlayControlsVisible() {
        return playButton().isVisible();
    }

    @Override
    public boolean isVisible() {
        return trackContainer().isVisible();
    }

    public MenuElement clickMenu() {
        menu().click();
        return new MenuElement(solo);
    }

    private ViewElement menu(){
       return solo.findElement(With.id(R.id.track_page_more));
    }
}
