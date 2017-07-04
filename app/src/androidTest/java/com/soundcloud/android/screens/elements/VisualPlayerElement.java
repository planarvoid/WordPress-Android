package com.soundcloud.android.screens.elements;

import com.robotium.solo.Condition;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.helpers.PlayerHelper;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.UpgradeScreen;
import com.soundcloud.android.screens.WhyAdsScreen;
import com.soundcloud.android.screens.WhyAdsUpsellScreen;

import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.concurrent.TimeUnit;

public class VisualPlayerElement extends Element {
    private static final int MILISECONDS_UNTIL_AUDIO_AD_DONE = (int) TimeUnit.SECONDS.toMillis(33L); // 30 secs + 3 buffering
    private static final int MILISECONDS_UNTIL_VIDEO_AD_DONE = (int) TimeUnit.SECONDS.toMillis(70L); // 67 secs + 3 buffering
    private static final int MIN_EXPANDED_HEIGHT = 100; // xxxhdpi status bar height


    private final With footerPlayerPredicate = With.id(R.id.footer_controls);
    private final Condition IS_EXPANDED_CONDITION = () -> player().isOnScreen() && isExpanded();
    private final Condition IS_COLLAPSED_CONDITION = () -> isCollapsed();

    public VisualPlayerElement(Han testDriver) {
        super(testDriver, With.id(R.id.player_layout));
    }

    public boolean isNextButtonClickable() {
        return testDriver.isElementDisplayed(With.id(R.id.player_next)) && nextButton().isOnScreen() && nextButton().isEnabled();
    }

    public boolean isPreviousButtonClickable() {
        return testDriver.isElementDisplayed(With.id(R.id.player_previous)) && previousButton().isOnScreen() && previousButton()
                .isEnabled();
    }

    public boolean isLeaveBehindVisible() {
        return leaveBehind().isOnScreen();
    }

    public boolean isInterstitialVisible() {
        return interstitial().isOnScreen();
    }

    public VisualPlayerElement waitForTheExpandedPlayerToPlayNextTrack() {
        waiter.waitForElementCondition(new TrackChangedCondition(getTrackTitle()));
        return this;
    }

    public VisualPlayerElement waitForTheExpandedPlayerToPlayNextTrack(int timeout) {
        waiter.waitForElementCondition(new TrackChangedCondition(getTrackTitle()), timeout);
        return this;
    }

    public VisualPlayerElement unlike() {
        if (likeButton().isChecked()) {
            likeButton().click();
        }
        return this;
    }

    public VisualPlayerElement startStationFromUnplayableTrack() {
        testDriver.findOnScreenElement(With.text(testDriver.getString(R.string.stations_start_track_station))).click();
        return this;
    }

    private ViewElement playButton() {
        return testDriver.findOnScreenElement(With.id(R.id.player_play));
    }

    private ViewElement previousButton() {
        return testDriver.findOnScreenElement(With.id(R.id.player_previous));
    }

    private ViewElement nextButton() {
        return testDriver.findOnScreenElement(With.id(R.id.player_next));
    }

    public boolean isSkippable() {
        return testDriver.isElementDisplayed(With.id(R.id.skip_ad)) && skipAd().isOnScreen();
    }

    private ViewElement skipAd() {
        return testDriver.findOnScreenElement(With.id(R.id.skip_ad));
    }

    private ViewElement trackContainer() {
        return testDriver.findOnScreenElement(With.id(R.id.player_track_pager));
    }

    private ViewElement closeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.player_close_indicator));
    }

    private ViewElement video() {
        return testDriver.findOnScreenElement(With.id(R.id.video_view));
    }

    private ViewElement artwork() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_artwork));
    }

    private ViewElement fullBleedArtwork() {
        return testDriver.findOnScreenElement(With.id(R.id.fullbleed_ad_artwork));
    }

    private ViewElement centeredAdArtwork() {
        return testDriver.findOnScreenElement(With.id(R.id.centered_ad_artwork));
    }

    private ViewElement creator() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_user));
    }

    private ViewElement trackPageContext() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_context));
    }

    private ViewElement footerUser() {
        return testDriver.findOnScreenElement(With.id(R.id.footer_user));
    }

    private ViewElement footerPlayToggle() {
        return testDriver.findOnScreenElement(With.id(R.id.footer_toggle));
    }

    private TextElement trackTitle() {
        return new TextElement(getTrackTitleViewElement());
    }

    private ViewElement footerPlayer() {
        return testDriver.findOnScreenElement(footerPlayerPredicate);
    }

    private ViewElement adCTAButton() {
        return testDriver.findOnScreenElement(With.id(R.id.cta_button));
    }

    private ViewElement whyAds() {
        return testDriver.findOnScreenElement(With.id(R.id.why_ads));
    }

    private ViewElement upgrade() {
        return testDriver.findOnScreenElement(With.id(R.id.upsell_button));
    }

    private ViewElement adPage() {
        return testDriver.findOnScreenElement(With.id(R.id.player_ad_page));
    }

    private ViewElement leaveBehind() {
        return testDriver.findOnScreenElement(With.id(R.id.leave_behind));
    }

    private ViewElement interstitial() {
        return testDriver.findOnScreenElement(With.id(R.id.interstitial));
    }

    private ViewElement toggleLike() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_like));
    }

    private ViewElement interstitialNowPlaying() {
        return testDriver.findOnScreenElement(With.id(R.id.interstitial_now_playing_title));
    }

    private ViewElement progress() {
        return testDriver.findOnScreenElement(With.id(R.id.timestamp_progress));
    }

    public boolean isExpanded() {
        int playerSize = getPlayerHeight() - getFullScreenHeight();
        return playerSize >= 0 && playerSize < MIN_EXPANDED_HEIGHT;
    }

    public boolean isCollapsed() {
        return footerPlayer().isOnScreen();
    }

    public boolean isExpandedPlayerPlaying() {
        return waitForExpandedPlayer().isExpanded()
                && !playButton().isOnScreen()
                && progress().isOnScreen()
                && waiter.waitForElementCondition(new TextChangedCondition(progress()));
    }

    public boolean isExpandedPlayerPaused() {
        return waitForExpandedPlayer().isExpanded() && playButton().isOnScreen();
    }


    public VisualPlayerElement tapFooter() {
        footerPlayer().click();
        waitForExpandedPlayer();
        return this;
    }

    public VisualPlayerElement pressBackToCollapse() {
        waitForExpandedPlayer();
        testDriver.goBack();
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

    public VisualPlayerElement swipeNext() {
        waitForContent();
        testDriver.swipeLeft();
        waiter.waitForPlayerPage();
        return this;
    }

    public VisualPlayerElement swipePrevious() {
        waitForContent();
        testDriver.swipeRight();
        waiter.waitForPlayerPage();
        return this;
    }

    @SuppressWarnings("unused")
    public WhyAdsScreen clickWhyAds() {
        whyAds().click();
        return new WhyAdsScreen(testDriver);
    }

    public WhyAdsUpsellScreen clickWhyAdsForUpsell() {
        whyAds().click();
        return new WhyAdsUpsellScreen(testDriver);
    }

    public UpgradeScreen clickUpgrade() {
        upgrade().click();
        return new UpgradeScreen(testDriver);
    }

    public boolean isAdPageVisible() {
        return adPage().isOnScreen();
    }

    public void clickAdCTAButton() {
        adCTAButton().click();
    }

    public String getAdCTAButtonText() {
        return new TextElement(adCTAButton()).getText();
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

    public String getTrackPageContext() {
        return new TextElement(trackPageContext()).getText();
    }

    public String getFooterTrackCreator() {
        return new TextElement(footerUser()).getText();
    }

    public boolean isFooterAdTextVisible() {
        return testDriver.findOnScreenElement(With.id(R.id.footer_ad_text)).hasVisibility();
    }

    public void waitForContent() {
        waiter.waitForElement(With.id(R.id.player_track_pager));
        waiter.waitForContent(getViewPager());
    }

    public void waitForMoreContent() {
        waiter.waitForNetworkCondition(() -> hasMoreTracks());
    }

    public boolean hasMoreTracks() {
        return getViewPager().getAdapter().getCount() > getViewPager().getCurrentItem();
    }

    public VisualPlayerElement waitForExpandedPlayer() {
        waiter.waitForElementCondition(IS_EXPANDED_CONDITION);
        return this;
    }

    private int getFullScreenHeight() {
        final View decorView = testDriver.getCurrentActivity().getWindow().getDecorView();
        final Rect dimens = new Rect();
        decorView.getWindowVisibleDisplayFrame(dimens);

        return dimens.bottom - dimens.top;
    }

    private int getPlayerHeight() {
        final ViewElement element = player();
        return element.getHeight() + element.getTop();
    }

    private ViewElement player() {
        return testDriver.findOnScreenElement(With.id(R.id.player_root));
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

    public void waitForAudioAdToBeDone() {
        testDriver.sleep(MILISECONDS_UNTIL_AUDIO_AD_DONE);
    }

    public void waitForVideoAdToBeDone() {
        testDriver.sleep(MILISECONDS_UNTIL_VIDEO_AD_DONE);
    }

    public VisualPlayerElement waitForSkipAdButton() {
        waiter.waitForElement(R.id.skip_ad);
        return this;
    }

    public boolean waitForInterstitialToLoad() {
        return waiter.waitForNetworkCondition(() -> testDriver.findOnScreenElement(With.id(R.id.interstitial)).hasVisibility());
    }

    public VisualPlayerElement waitForLeaveBehindToLoad() {
        waiter.waitForElement(R.id.leave_behind_image);
        return this;
    }

    public void waitForPlayButton() {
        waiter.waitForElement(R.id.player_play);
    }

    public boolean waitForAdPage() {
        return waiter.waitForElement(R.id.player_ad_page);
    }

    public VisualPlayerElement waitForExpandedPlayerToStartPlaying() {
        waiter.waitForElementToBeInvisible(With.id(R.id.player_play));
        return this;
    }

    public boolean waitForPlayState() {
        if (isExpanded()) {
            return waiter.waitForElementToBeInvisible(With.id(R.id.player_play));
        } else {
            return waiter.waitForElementToBeChecked(With.id(R.id.footer_toggle));
        }
    }

    private ViewPager getViewPager() {
        return testDriver.findOnScreenElement(With.id(R.id.player_track_pager)).toViewPager();
    }

    public VisualPlayerElement toggleFooterPlay() {
        footerPlayToggle().click();
        return this;
    }

    public VisualPlayerElement clickArtwork() {
        artwork().click();
        return this;
    }

    public void clickAdVideo() {
        video().click();
    }

    public void clickAdArtwork() {
        if (isCenteredAd()) {
            clickCenteredAdArtwork();
        } else {
            clickFullbleedAdArtwork();
        }
    }

    public ViewElement likeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_like));
    }

    public ViewElement shareButton() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_share));
    }

    public boolean isCenteredAd() {
        return centeredAdArtwork().isOnScreen();
    }

    public String error() {
        return errorElement().getText();
    }

    public boolean isErrorBlockedVisible() {
        return testDriver.findOnScreenElement(With.id(R.id.playback_error_blocked)).hasVisibility();
    }

    private TextElement errorElement() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.playback_error)));
    }

    private TextElement errorReasonElement() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.playback_error_reason)));
    }

    public void clickCenteredAdArtwork() {
        centeredAdArtwork().click();
    }

    public boolean isFullbleedAd() {
        return fullBleedArtwork().isOnScreen();
    }

    public void clickFullbleedAdArtwork() {
        fullBleedArtwork().click();
    }

    public ProfileScreen clickCreator() {
        waitForExpandedPlayer();
        creator().click();
        return new ProfileScreen(testDriver);
    }

    @Override
    public boolean isVisible() {
        return trackContainer().isOnScreen();
    }

    public PlayerMenuElement clickMenu() {
        menu().click();
        return new PlayerMenuElement(testDriver);
    }

    public void tapToggleLikeButton() {
        waitForExpandedPlayer();
        toggleLike().click();
    }

    private ViewElement menu() {
        return testDriver.findOnScreenElement(With.id(R.id.track_page_more));
    }

    public void playForFiveSeconds() {
        testDriver.sleep(5000);
    }

    public ViewElement getTrackTitleViewElement() {
        return testDriver.findOnScreenElementWithPopulatedText(With.id(R.id.track_page_title));
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

    public PlayQueueElement pressPlayQueueButton() {
        playQueueButton().click();
        return new PlayQueueElement(testDriver);
    }

    private ViewElement playQueueButton() {
        return testDriver.findOnScreenElement(With.id(R.id.play_queue_button));
    }

    private class TrackChangedCondition implements Condition {

        private final String original;

        private TrackChangedCondition(String original) {
            this.original = original;
        }

        @Override
        public boolean isSatisfied() {
            return PlayerHelper.isNotCurrentTrack(VisualPlayerElement.this, original);
        }
    }
}
