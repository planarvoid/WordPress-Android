package com.soundcloud.android.screens.elements;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.TextElement;
import com.soundcloud.android.tests.viewelements.ViewElement;
import com.soundcloud.android.tests.with.With;

import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class VisualPlayerElement extends Element {
    private static final int MILISECONDS_UNTIL_AD_DONE =  (int) TimeUnit.SECONDS.toMillis(33L); // 30 secs + 3 buffering

    private final With footerPlayerPredicate = With.id(R.id.footer_controls);
    private final Condition IS_EXPANDED_CONDITION = new Condition() {
        @Override
        public boolean isSatisfied() {
            return isExpanded();
        }
    };
    private final Condition IS_COLLAPSED_CONDITION = new Condition() {
        @Override
        public boolean isSatisfied() {
            return isCollapsed();
        }
    };

    public VisualPlayerElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return R.id.player_layout;
    }

    public boolean isNextButtonClickable() {
        return solo.isElementDisplayed(With.id(R.id.player_next)) && nextButton().isVisible() && nextButton().isEnabled();
    }

    public boolean isPreviousButtonClickable() {
        return solo.isElementDisplayed(With.id(R.id.player_previous)) && previousButton().isVisible() && previousButton().isEnabled();
    }

    public boolean isLeaveBehindVisible() {
        return leaveBehind().isVisible();
    }

    public boolean isInterstitialVisible() {
        return interstitial().isVisible();
    }

    private ViewElement previousButton() {
        return solo.findElement(With.id(R.id.player_previous));
    }

    private ViewElement nextButton() {
        return solo.findElement(With.id(R.id.player_next));
    }

    public boolean isSkippable() {
        return solo.isElementDisplayed(With.id(R.id.skip_ad)) && skipAd().isVisible();
    }

    private ViewElement skipAd() {
        return solo.findElement(With.id(R.id.skip_ad));
    }

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

    private ViewElement footerUser() {
        return solo.findElement(With.id(R.id.footer_user));
    }

    private ViewElement footerPlayToggle() {
        return solo.findElement(With.id(R.id.footer_toggle));
    }

    private TextElement trackTitle() {
        return new TextElement(solo.findElement(With.id(R.id.track_page_title)));
    }

    private ViewElement footerPlayer() {
        return solo.findElement(footerPlayerPredicate);
    }

    private ViewElement whyAds() {
        return solo.findElement(With.id(R.id.why_ads));
    }

    private ViewElement adPage() {
        return solo.findElement(With.id(R.id.player_ad_page));
    }

    private ViewElement leaveBehind() {
        return solo.findElement(With.id(R.id.leave_behind));
    }

    private ViewElement interstitial() {
        return solo.findElement(With.id(R.id.interstitial));
    }

    private ViewElement interstitialNowPlaying() {
        return solo.findElement(With.id(R.id.interstitial_now_playing_title));
    }

    public boolean isExpanded() {
        return  getPlayerHeight() - getFullScreenHeight() == 0;
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

    public WhyAdsScreen clickWhyAds() {
        whyAds().click();
        return new WhyAdsScreen(solo);
    }

    public boolean isAdPageVisible() {
        return adPage().isVisible();
    }

    public String getTrackTitle() {
        return trackTitle().getText();
    }

    public String interstitalNowPlayingText() {
        return new TextElement(interstitialNowPlaying()).getText();
    }

    public String getTrackCreator() {
        return new TextElement(creator()).getText();
    }

    public String getFooterTrackCreator() {
        return new TextElement(footerUser()).getText();
    }

    public void waitForContent() {
        waiter.waitForContent(getViewPager());
    }

    public boolean waitForExpandedPlayer() {
        return waiter.waitForElementCondition(IS_EXPANDED_CONDITION);
    }

    private int getFullScreenHeight() {
        final View decorView = solo.getCurrentActivity().getWindow().getDecorView();
        final Rect dimens = new Rect();
        decorView.getWindowVisibleDisplayFrame(dimens);

        return dimens.bottom - dimens.top;
    }

    private int getPlayerHeight() {
        final ViewElement element = solo.findElement(With.id(R.id.player_root));
        return element.getHeight() + element.getTop();
    }

    public boolean waitForCollapsedPlayer() {
        return waiter.waitForElementCondition(IS_COLLAPSED_CONDITION);
    }

    public void waitForAdToBeFetched() {
        waiter.waitFiveSeconds();
    }

    public void waitForAdToBeSkippable() {
        waiter.waitForAdToBeComeSkippable(With.id(R.id.skip_ad));
    }

    public void waitForAdToBeDone() {
        solo.sleep(MILISECONDS_UNTIL_AD_DONE);
    }

    public void waitForTrackToFinish(int firstTrackLengthInMiliSeconds) {
        solo.sleep(firstTrackLengthInMiliSeconds);
    }

    public void waitForSkipAdButton() {
        waiter.waitForElement(R.id.skip_ad);
    }

    public void waitForAdOverlayToLoad() {
        waiter.waitFiveSeconds();
    }

    public void waitForPlayButton() {
        waiter.waitForElement(R.id.player_play);
    }

    public void waitForAdPage() {
        waiter.waitForElement(R.id.player_ad_page);
    }

    public boolean waitForPlayState() {
        if (isExpanded()) {
            return waiter.waitForElementToBeInvisible(With.id(R.id.player_play));
        } else {
            return waiter.waitForElementToBeChecked(With.id(R.id.footer_toggle));
        }
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
        waitForExpandedPlayer();
        creator().click();
        return new ProfileScreen(solo);
    }

    @Override
    public boolean isVisible() {
        return trackContainer().isVisible();
    }

    public PlayerMenuElement clickMenu() {
        menu().click();
        return new PlayerMenuElement(solo);
    }

    private ViewElement menu() {
        return solo.findElement(With.id(R.id.track_page_more));
    }
}
