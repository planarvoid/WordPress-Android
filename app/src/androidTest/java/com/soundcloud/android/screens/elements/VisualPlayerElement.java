package com.soundcloud.android.screens.elements;

import static junit.framework.Assert.assertTrue;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.WhyAdsScreen;

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
        super(solo, With.id(R.id.player_layout));
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

    public void waitForTheExpandedPlayerToPlayNextTrack() {
        waiter.waitForElementCondition(new TrackChangedCondition(getTrackTitle()));
        assertTrue(isExpandedPlayerPlaying());
    }

    private ViewElement playButton() {
        return solo.findElement(With.id(R.id.player_play));
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

    private ViewElement fullBleedArtwork() {
        return solo.findElement(With.id(R.id.fullbleed_ad_artwork));
    }

    private ViewElement centeredAdArtwork() {
        return solo.findElement(With.id(R.id.centered_ad_artwork));
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

    private ViewElement toggleLike() {
        return solo.findElement(With.id(R.id.track_page_like));
    }

    private ViewElement interstitialNowPlaying() {
        return solo.findElement(With.id(R.id.interstitial_now_playing_title));
    }

    private ViewElement progress() {
        return solo.findElement(With.id(R.id.timestamp_progress));
    }

    public boolean isExpanded() {
        return  getPlayerHeight() - getFullScreenHeight() == 0;
    }

    public boolean isCollapsed() {
        return footerPlayer().isVisible();
    }

    public boolean isExpandedPlayerPlaying() {
        return waitForExpandedPlayer()
                && !playButton().isVisible()
                && progress().isVisible()
                && waiter.waitForElementCondition(new TextChangedCondition(progress()));
    }

    public void tapFooter() {
        footerPlayer().click();
        waitForExpandedPlayer();
    }

    public VisualPlayerElement pressBackToCollapse() {
        waitForExpandedPlayer();
        solo.goBack();
        waitForCollapsedPlayer();
        return this;
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
        waitForContent();
        solo.swipeLeft();
        waiter.waitForPlayerPage();
    }

    public void swipePrevious() {
        waitForContent();
        solo.swipeRight();
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

    public VisualPlayerElement waitForAdToBeSkippable() {
        waiter.waitForAdToBeComeSkippable(With.id(R.id.skip_ad));
        return this;
    }

    public void waitForAdToBeDone() {
        solo.sleep(MILISECONDS_UNTIL_AD_DONE);
    }

    public VisualPlayerElement waitForSkipAdButton() {
        waiter.waitForElement(R.id.skip_ad);
        return this;
    }

    public void waitForAdOverlayToLoad() {
        waiter.waitFiveSeconds();
    }

    public void waitForPlayButton() {
        waiter.waitForElement(R.id.player_play);
    }

    public boolean waitForAdPage() {
        return waiter.waitForElement(R.id.player_ad_page);
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

    public void clickAdArtwork() {
        if (isCenteredAd()) {
            clickCenteredAdArtwork();
        } else {
            clickFullbleedAdArtwork();
        }
    }

    public boolean isCenteredAd() {
       return centeredAdArtwork().isVisible();
    }

    public void clickCenteredAdArtwork() {
       centeredAdArtwork().click();
    }

    public boolean isFullbleedAd() {
        return fullBleedArtwork().isVisible();
    }

    public void clickFullbleedAdArtwork() {
        fullBleedArtwork().click();
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

    public void tapToggleLikeButton() {
        waitForExpandedPlayer();
        toggleLike().click();
    }

    private ViewElement menu() {
        return solo.findElement(With.id(R.id.track_page_more));
    }

    public void playForFiveSeconds() {
        solo.sleep(5000);
    }

    private static class TextChangedCondition implements Condition {

        private final String original;
        private final TextElement textElement;

        private TextChangedCondition(ViewElement textElement) {
            this.textElement = new TextElement(textElement);
            original = this.textElement.getText();
        }

        @Override
        public boolean isSatisfied() {
            return !textElement.getText().equals(original);
        }
    }

    private class TrackChangedCondition implements Condition {

        private final String original;

        private TrackChangedCondition(String original) {
            this.original = original;
        }

        @Override
        public boolean isSatisfied() {
            return !trackTitle().equals(original);
        }
    }
}
