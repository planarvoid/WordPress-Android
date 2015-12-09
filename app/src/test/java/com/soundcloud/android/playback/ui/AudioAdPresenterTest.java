package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.Player.PlayerState;
import static com.soundcloud.android.playback.Player.Reason;
import static com.soundcloud.android.playback.Player.StateTransition;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import rx.Observable;

public class AudioAdPresenterTest extends AndroidUnitTest {

    private AudioAdPresenter presenter;
    private View adView;

    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private SkipListener skipListener;

    @Before
    public void setUp() throws Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(mock(PlayerOverlayController.class));
        when(imageOperations.adImage(any(Uri.class))).thenReturn(Observable.<Bitmap>empty());

        presenter = new AudioAdPresenter(imageOperations, resources(), playerOverlayControllerFactory, pageListener);
        adView = presenter.createItemView(new FrameLayout(context()), skipListener);
        presenter.bindItemView(adView, new PlayerAd(buildAd(), buildTrack()));
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
        adView.findViewById(R.id.centered_ad_artwork).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void toggleClickThroughOnCenteredOverlayClick() {
        adView.findViewById(R.id.centered_ad_overlay).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void playerCloseOnPlayerCloseClick() {
        adView.findViewById(R.id.player_close).performClick();

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
        adView.findViewById(R.id.cta_button).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void callToActionTextShouldHaveDefaultTextIfNotProvided() {
        final Button button = (Button) adView.findViewById(R.id.cta_button);

        assertThat(button).hasText("LEARN MORE");
    }

    @Test
    public void callToActionTextShouldUseProvidedText() {
        final String expectedText = "CLICK ME!!!";
        final PlayerAdData adData = prepareAd(AdFixtures.getAudioAdWithCustomCTA(expectedText, Urn.forTrack(123L)));
        presenter.bindItemView(adView, new PlayerAd(adData, buildTrack()));

        final Button button = (Button) adView.findViewById(R.id.cta_button);

        assertThat(button).hasText(expectedText);
    }

    @Test
    public void clickSkipAdShouldSkipAd() {
        adView.findViewById(R.id.skip_ad).performClick();

        verify(pageListener).onSkipAd();
    }

    @Test
    public void showAboutAdsOnWhyAdsClick() {
        adView.findViewById(R.id.why_ads).performClick();

        verify(pageListener).onAboutAds(any(FragmentActivity.class));
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
        when(imageOperations.adImage(any(Uri.class))).thenReturn(Observable.just((Bitmap) null));
        presenter.bindItemView(adView, new PlayerAd(buildAd(), buildTrack()));

        assertCenteredLayoutInvisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void centeredLayoutSetOnIABSizedAdImage() {
        when(imageOperations.adImage(any(Uri.class))).thenReturn(Observable.just(buildBitmap(300, 250)));
        presenter.bindItemView(adView, new PlayerAd(buildAd(), buildTrack()));

        assertCenteredLayoutVisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void centeredLayoutSetOn2xIABSizedAdImage() {
        when(imageOperations.adImage(any(Uri.class))).thenReturn(Observable.just(buildBitmap(600, 500)));
        presenter.bindItemView(adView, new PlayerAd(buildAd(), buildTrack()));

        assertCenteredLayoutVisible();
        assertFullbleedLayoutInvisible();
    }

    @Test
    public void fullbleedLayoutSetOnLargerThan2xIABSizedAdImage() {
        when(imageOperations.adImage(any(Uri.class))).thenReturn(Observable.just(buildBitmap(601, 501)));
        presenter.bindItemView(adView, new PlayerAd(buildAd(), buildTrack()));

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
    public void setPlayingStateShouldHidePlayControls() {
        presenter.setPlayState(adView, new StateTransition(PlayerState.PLAYING, Reason.NONE, Urn.forTrack(123L)), true, true);
        assertThat(adView.findViewById(R.id.play_controls)).isGone();
    }

    @Test
    public void setBufferingStateShouldHidePlayControls() {
        presenter.setPlayState(adView, new StateTransition(PlayerState.BUFFERING, Reason.NONE, Urn.forTrack(123L)), true, true);
        assertThat(adView.findViewById(R.id.play_controls)).isGone();
    }

    @Test
    public void setIdleStateShouldShowPlayControls() {
        presenter.setPlayState(adView, new StateTransition(PlayerState.IDLE, Reason.NONE, Urn.forTrack(123L)), true, true);
        assertThat(adView.findViewById(R.id.play_controls)).isVisible();
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return new PlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration));
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
        assertThat(adView.findViewById(R.id.centered_ad_overlay)).isVisible();
        assertThat(adView.findViewById(R.id.centered_ad_artwork)).isVisible();
    }

    private void assertCenteredLayoutInvisible() {
        assertThat(adView.findViewById(R.id.centered_ad_overlay)).isInvisible();
        assertThat(adView.findViewById(R.id.centered_ad_artwork)).isInvisible();
    }

    private Bitmap buildBitmap(int width, int height) {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    private PlayerAdData buildAd() {
        return prepareAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
    }

    private PlayerAdData prepareAd(AudioAd audioAd) {
        audioAd.setMonetizableTitle("track");
        audioAd.setMonetizableCreator("artist");
        return audioAd;
    }

    private PropertySet buildTrack() {
        return PropertySet.from(
                PlayableProperty.TITLE.bind("Ad Title"),
                TrackProperty.URN.bind(Urn.forTrack(123L))
        );
    }
}
