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
import com.soundcloud.android.ads.AdProperty;
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

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class AdPagePresenterTest extends AndroidUnitTest {

    private AdPagePresenter presenter;

    private View adView;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private SkipListener skipListener;

    @Before
    public void setUp() throws Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(mock(PlayerOverlayController.class));
        presenter = new AdPagePresenter(imageOperations, resources(), playerOverlayControllerFactory, pageListener, context());
        adView = presenter.createItemView(new FrameLayout(context()), skipListener);
        presenter.bindItemView(adView, new PlayerAd(buildAd()));
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
    public void togglePlayOnAdPageArtworkClick() {
        adView.findViewById(R.id.track_page_artwork).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnAdPageArtworkOverlayClick() {
        adView.findViewById(R.id.artwork_overlay).performClick();

        verify(pageListener).onTogglePlay();
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
    public void clickThroughOnLearnMoreClick() {
        adView.findViewById(R.id.learn_more).performClick();

        verify(pageListener).onClickThrough();
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

    private PropertySet buildAd() {
        return PropertySet.from(
                PlayableProperty.TITLE.bind("Ad Title"),
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                AdProperty.ARTWORK.bind(Uri.EMPTY),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.EMPTY),
                AdProperty.MONETIZABLE_TRACK_URN.bind(Urn.forTrack(123L)),
                AdProperty.MONETIZABLE_TRACK_TITLE.bind("track"),
                AdProperty.MONETIZABLE_TRACK_CREATOR.bind("artist"),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#111111"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#333333"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#444444"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#555555"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#666666")
        );
    }
}
