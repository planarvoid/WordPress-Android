package com.soundcloud.android.playback.ui;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class AudioAdPresenterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private AudioAdPresenter presenter;
    private View adView;

    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private SkipListener skipListener;
    @Mock private PlayerArtworkLoader artworkLoader;

    @Before
    public void setUp() throws Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(playerOverlayController);
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.<Bitmap>empty());

        presenter = new AudioAdPresenter(imageOperations, resources(), playerOverlayControllerFactory, pageListener, artworkLoader);
        adView = presenter.createItemView(new FrameLayout(context()), skipListener);
        bindSkippableAd();
    }

    @Test
    public void togglePlayOnFooterToggleClick() {
        adView.findViewById(R.id.footer_toggle).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnPlayClick() {
        adView.findViewById(R.id.player_play).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnFullBleedAdPageArtworkClick() {
        adView.findViewById(R.id.fullbleed_ad_artwork).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnAdPageOverlay() {
        adView.findViewById(R.id.artwork_overlay).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void toggleClickThroughOnCenteredAdPageArtworkClick() {
        final View view = adView.findViewById(R.id.centered_ad_artwork);
        view.performClick();

        verify(pageListener).onClickThrough(view.getContext());
    }

    @Test
    public void toggleClickThroughOnCenteredOverlayClick() {
        final View view = adView.findViewById(R.id.centered_ad_clickable_overlay);
        view.performClick();

        verify(pageListener).onClickThrough(view.getContext());
    }

    @Test
    public void playerCloseOnPlayerCloseClick() {
        adView.findViewById(R.id.player_expanded_top_bar).performClick();

        verify(pageListener).onPlayerClose();
    }

    @Test
    public void playerCloseOnPreviewContainerClick() {
        adView.findViewById(R.id.preview_container).performClick();

        verify(pageListener).onPlayerClose();
    }

    @Test
    public void footerTapOnFooterControlsClick() {
        adView.findViewById(R.id.footer_controls).performClick();

        verify(pageListener).onFooterTap();
    }

    @Test
    public void clickThroughOnCallToActionClick() {
        final View view = adView.findViewById(R.id.cta_button);
        view.performClick();

        verify(pageListener).onClickThrough(view.getContext());
    }

    @Test
    public void callToActionTextShouldHaveDefaultTextIfNotProvided() {
        final Button button = (Button) adView.findViewById(R.id.cta_button);

        assertThat(button).hasText("LEARN MORE");
    }

    @Test
    public void callToActionTextShouldUseProvidedText() {
        final String expectedText = "CLICK ME!!!";
        final AudioAd adData = prepareAd(AdFixtures.getAudioAdWithCustomCTA(expectedText, Urn.forTrack(123L)));
        presenter.bindItemView(adView, new AudioPlayerAd(adData));

        final Button button = (Button) adView.findViewById(R.id.cta_button);

        assertThat(button).hasText(expectedText);
    }

    @Test
    public void conpanionlessTextIsNotVisibleIfCompanionExists() {
        bindSkippableAd();

        assertThat(adView.findViewById(R.id.companionless_ad_text)).isNotVisible();
    }

    @Test
    public void companionlessTextIsVisibleIfNoCompanion() {
        when(artworkLoader.loadAdBackgroundImage(TRACK_URN)).thenReturn(Observable.<Bitmap>empty());

        bindCompanionlessAd();

        assertThat(adView.findViewById(R.id.companionless_ad_text)).isVisible();
    }

    @Test
    public void blurredNextTrackArtIsSetAsBackgroundIfNoCompanion() {
        PublishSubject subject = PublishSubject.<Bitmap>create();
        when(artworkLoader.loadAdBackgroundImage(TRACK_URN)).thenReturn(subject);

        bindCompanionlessAd();

        verify(playerOverlayController).setAdOverlayShown(true);
        org.assertj.core.api.Assertions.assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void clickSkipAdShouldSkipAd() {
        adView.findViewById(R.id.skip_ad).performClick();

        verify(pageListener).onSkipAd();
    }

    @Test
    public void showAboutAdsOnWhyAdsClick() {
        adView.findViewById(R.id.why_ads).performClick();

        verify(pageListener).onAboutAds(any());
    }

    @Test
    public void bothLayoutsInvisibleOnClearedAdView() {
        presenter.clearItemView(adView);

        assertCenteredLayoutInvisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void bothLayoutsInvisibleOnNoAdImageObserved() {
        assertCenteredLayoutInvisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void bothLayoutsInvisibleOnNullAdImage() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(null));
        bindSkippableAd();

        assertCenteredLayoutInvisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void centeredLayoutSetOnIABSizedAdImage() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(buildBitmap(300, 250)));
        bindSkippableAd();

        assertCenteredLayoutVisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void centeredLayoutSetOn2xIABSizedAdImage() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(buildBitmap(600, 500)));
        bindSkippableAd();

        assertCenteredLayoutVisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void centeredAdWithoutClickthroughIsNotClickable() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(buildBitmap(300, 250)));
        bindNonClickableAd();

        assertThat(adView.findViewById(R.id.centered_ad_clickable_overlay)).isGone();
        assertThat(adView.findViewById(R.id.centered_ad_artwork)).isVisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void fullbleedAdWithoutClickthroughDoesNotShowCallToAction() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(buildBitmap(601, 501)));
        bindNonClickableAd();

        assertCenteredLayoutInvisible();
        assertThat(adView.findViewById(R.id.cta_button)).isGone();
        assertThat(adView.findViewById(R.id.fullbleed_ad_artwork)).isVisible();
    }

    @Test
    public void fullbleedLayoutSetOnLargerThan2xIABSizedAdImage() {
        when(imageOperations.bitmap(any(Uri.class))).thenReturn(Observable.just(buildBitmap(601, 501)));
        bindSkippableAd();

        assertCenteredLayoutInvisible();
        assertFullbleedLayoutVisible();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionOnClickingUnexpectedView() {
        presenter.onClick(mock(View.class));
    }

    @Test
    public void setProgressShouldInitiallySetATimerTo15sec() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 0, 30));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("Skip in: 15 sec.");
    }

    @Test
    public void setProgressShouldUpdateTimeUntilSkipAccordingToPosition() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 7, 30));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("Skip in: 8 sec.");
    }

    @Test
    public void setProgressShouldDisplayEnableSkipAdAfter15sec() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 15, 30));

        assertThat(timeUntilSkip()).isGone();
        assertThat(previewArtworkOverlay()).isGone();
        assertThat(skipAd()).isVisible();
    }

    @Test
    public void setProgressForUnskippableAdShouldInitiallySetTimerToDuration() {
        bindUnskippableAd();
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 0, 30));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("30 sec.");
    }

    @Test
    public void setProgressForUnskippableAdShouldUpdateDurationRemainingAccordingToPosition() {
        bindUnskippableAd();
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 7, 30));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("23 sec.");
    }

    @Test
    public void setProgressShouldNotEnableSkipAdAfter15secForUnskippableAd() {
        bindUnskippableAd();
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 15, 30));

        assertThat(timeUntilSkip()).isVisible();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(skipAd()).isGone();
    }

    @Test
    public void setProgressForAdLessThan15SecsShouldInitiallySetATimerToDuration() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 0, 10));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("10 sec.");
    }

    @Test
    public void setProgressForAdLessThan15SecsShouldUpdateDurationRemainingAccordingToPosition() {
        presenter.setProgress(adView, createProgress(TimeUnit.SECONDS, 7, 10));

        assertThat(skipAd()).isGone();
        assertThat(previewArtworkOverlay()).isVisible();
        assertThat(timeUntilSkip()).isVisible();
        assertThat(timeUntilSkip()).containsText("3 sec.");
    }

    @Test
    public void setPlayingStateShouldHidePlayControls() {
        presenter.setPlayState(adView,
                               TestPlayStates.playing(),
                               true,
                               true);
        assertThat(adView.findViewById(R.id.play_controls)).isGone();
    }

    @Test
    public void setBufferingStateShouldHidePlayControls() {
        presenter.setPlayState(adView,
                               TestPlayStates.buffering(),
                               true,
                               true);
        assertThat(adView.findViewById(R.id.play_controls)).isGone();
    }

    @Test
    public void setIdleStateShouldShowPlayControls() {
        presenter.setPlayState(adView,
                               TestPlayStates.idle(),
                               true,
                               true);
        assertThat(adView.findViewById(R.id.play_controls)).isVisible();
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return new PlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration), TRACK_URN);
    }

    private TextView timeUntilSkip() {
        return (TextView) adView.findViewById(R.id.time_until_skip);
    }

    private View skipAd() {
        return adView.findViewById(R.id.skip_ad);
    }

    private View previewArtworkOverlay() {
        return adView.findViewById(R.id.preview_artwork_overlay);
    }

    private void assertFullbleedLayoutVisible() {
        assertThat(adView.findViewById(R.id.cta_button)).isVisible();
        assertThat(adView.findViewById(R.id.fullbleed_ad_artwork)).isVisible();
    }

    private void assertFullbleedLayoutInvisible() {
        assertThat(adView.findViewById(R.id.cta_button)).isInvisible();
        assertThat(adView.findViewById(R.id.fullbleed_ad_artwork)).isInvisible();
    }

    private void assertCenteredLayoutVisible() {
        assertThat(adView.findViewById(R.id.centered_ad_clickable_overlay)).isVisible();
        assertThat(adView.findViewById(R.id.centered_ad_artwork)).isVisible();
    }

    private void assertCenteredLayoutInvisible() {
        assertThat(adView.findViewById(R.id.centered_ad_clickable_overlay)).isInvisible();
        assertThat(adView.findViewById(R.id.centered_ad_artwork)).isInvisible();
    }

    private Bitmap buildBitmap(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    private AudioAd buildAd(boolean skippable) {
        final AudioAd audioAd = skippable ?
                                AdFixtures.getAudioAd(TRACK_URN) : AdFixtures.getNonskippableAudioAd(TRACK_URN);
        return prepareAd(audioAd);
    }

    private AudioAd prepareAd(AudioAd audioAd) {
        audioAd.setMonetizableTitle("track");
        audioAd.setMonetizableCreator("artist");
        return audioAd;
    }

    private void bindSkippableAd() {
        presenter.bindItemView(adView, new AudioPlayerAd(buildAd(true)));
    }

    private void bindUnskippableAd() {
        presenter.bindItemView(adView, new AudioPlayerAd(buildAd(false)));
    }

    private void bindNonClickableAd() {
        presenter.bindItemView(adView, new AudioPlayerAd(AdFixtures.getNonClickableAudioAd(TRACK_URN)));
    }

    private void bindCompanionlessAd() {
        presenter.bindItemView(adView, new AudioPlayerAd(AdFixtures.getCompanionlessAudioAd(TRACK_URN)));
    }

}
