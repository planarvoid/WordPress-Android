package com.soundcloud.android.playback.ui;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.ApiVideoSource;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.Player.PlayerState;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerVideoAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.PropertySet;

import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoAdPresenterTest extends AndroidUnitTest {

    private VideoAdPresenter presenter;
    private View adView;

    private static final int VERTICAL_VIDEO_WIDTH = 600;
    private static final int VERTICAL_VIDEO_HEIGHT = 1024;
    private static final int LETTERBOX_VIDEO_WIDTH = 250;
    private static final int LETTERBOX_VIDEO_HEIGHT = 100;

    @Mock private MediaPlayerVideoAdapter mediaPlayerVideoAdapter;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private DeviceHelper deviceHelper;

    @Before
    public void setUp() throws Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(mock(PlayerOverlayController.class));
        when(deviceHelper.getCurrentOrientation()).thenReturn(ORIENTATION_PORTRAIT);

        presenter = new VideoAdPresenter(mediaPlayerVideoAdapter, imageOperations, pageListener, playerOverlayControllerFactory, deviceHelper, resources());
        adView = presenter.createItemView(new FrameLayout(context()), null);
        bindVerticalVideo();
    }

    @Test
    public void videoViewNotVisibleOnVerticalVideoBind() {
        bindVerticalVideo();

        assertThat(adView.findViewById(R.id.video_view)).isNotVisible();
    }

    @Test
    public void letterboxVideoBindShowsLoadingIndicatorAndLetterboxBackgroundButNotVideoView() {
        bindLetterboxVideo();

        assertThat(adView.findViewById(R.id.video_view)).isNotVisible();
        assertThat(adView.findViewById(R.id.letterbox_background)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void loadingIndicatorVisibleOnVerticalVideoBind() {
        bindVerticalVideo();

        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void videoViewShouldBeVisibleAndLoadingIndicatorGoneAfterPlaybackStarts() {
        bindLetterboxVideo();

        presenter.setPlayState(adView, createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);
        assertThat(adView.findViewById(R.id.video_view)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void loadingIndicatorAndVideoViewAreVisibleOnPlaybackBuffering() {
        bindLetterboxVideo();

        presenter.setPlayState(adView, createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);
        presenter.setPlayState(adView, createStateTransition(PlayerState.BUFFERING, Player.Reason.NONE), true, true);

        assertThat(adView.findViewById(R.id.video_view)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void loadingIndicatorIsntVisibleWhenPlaybackPaused() {
        bindLetterboxVideo();

        presenter.setPlayState(adView, createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);

        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void videoViewAspectRatioMaintainedForLetterbox() {
        bindLetterboxVideo();
        final ViewGroup.LayoutParams layoutParams = adView.findViewById(R.id.video_view).getLayoutParams();
        final int viewWidth = layoutParams.width;
        final int viewHeight = layoutParams.height;

        final float newAspectRatio = (float) viewWidth / (float) viewHeight;
        final float originalAspectRatio = (float) LETTERBOX_VIDEO_WIDTH / (float) LETTERBOX_VIDEO_HEIGHT;

        assertThat(newAspectRatio).isEqualTo(originalAspectRatio, Offset.offset(0.001F));
    }

    @Test
    public void videoViewAspectRatioMaintainedForLandscape() {
        when(deviceHelper.getCurrentOrientation()).thenReturn(ORIENTATION_LANDSCAPE);
        bindLetterboxVideo();

        final ViewGroup.LayoutParams layoutParams = adView.findViewById(R.id.video_view).getLayoutParams();
        final int viewWidth = layoutParams.width;
        final int viewHeight = layoutParams.height;

        final float newAspectRatio = (float) viewWidth / (float) viewHeight;
        final float originalAspectRatio = (float) LETTERBOX_VIDEO_WIDTH / (float) LETTERBOX_VIDEO_HEIGHT;

        assertThat(newAspectRatio).isEqualTo(originalAspectRatio, Offset.offset(0.001F));
    }

    @Test
    public void videoViewAspectRatioMaintainedForVerticalVideo() {
        final ViewGroup.LayoutParams layoutParams = adView.findViewById(R.id.video_view).getLayoutParams();
        final float newAspectRatio = (float) layoutParams.width / (float) layoutParams.height;
        final float originalAspectRatio = (float) VERTICAL_VIDEO_WIDTH / (float) VERTICAL_VIDEO_HEIGHT;

        assertThat(newAspectRatio).isEqualTo(originalAspectRatio, Offset.offset(0.001F));
    }

    @Test
    public void videoViewCoversEntireScreenForVerticalVideo() {
        final int viewWidth = adView.findViewById(R.id.video_view).getWidth();
        final int viewHeight = adView.findViewById(R.id.video_view).getHeight();

        final ViewGroup.LayoutParams layoutParams = adView.findViewById(R.id.video_view).getLayoutParams();
        assertThat(layoutParams.width).isGreaterThanOrEqualTo(resources().getDisplayMetrics().widthPixels);
        assertThat(layoutParams.height).isGreaterThanOrEqualTo(resources().getDisplayMetrics().heightPixels);
    }

    @Test
    public void fadingViewsWithoutPauseButtonVisibleOnBind() {
        for (View view : fadingViews()) {
            assertThat(view).isVisible();
        }
        assertThat(adView.findViewById(R.id.video_pause_control)).isNotVisible();
    }

    @Test
    public void fadingViewsWithoutPauseButtonVisibleOnClear() {
        presenter.clearItemView(adView);

        for (View view : fadingViews()) {
            assertThat(view).isVisible();
        }
        assertThat(adView.findViewById(R.id.video_pause_control)).isNotVisible();
    }

    @Test
    public void fadingViewsAreInvisibleAfterPlaybackStarts() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);

        for (View view : fadingViews()) {
            assertThat(view).isInvisible();
        }
    }

    @Test
    public void fadingViewsAreNotInvisibleAfterPlaybackStartsForNonCurrentItem() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), false, true);

        for (View view : fadingViews()) {
            assertThat(view).isNotInvisible();
        }
    }

    @Test
    public void fadingViewsAreVisibleOnPlaybackPause() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);

        for (View view : fadingViews()) {
            assertThat(view).isVisible();
        }
    }

    @Test
    public void fadingViewsWithPauseAreSetToInvisibleAfterPlayEventFromPause() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);

        for (View view : fadingViewsWithPause()) {
            assertThat(view).isInvisible();
        }
    }

    @Test
    public void togglePlayOnPlayClick() {
        adView.findViewById(R.id.player_play).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void togglePlayOnPauseClick() {
        adView.findViewById(R.id.video_pause_control).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void animatePauseButtonOnAdPageOverlayClick() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);
        adView.findViewById(R.id.video_overlay).performClick();

        assertThat(adView.findViewById(R.id.video_pause_control).getAnimation()).isNotNull();

    }

    @Test
    public void dontDisplayPauseButtonWhenAdPageOverlayClickedWhilePlaybackPaused() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);

        adView.findViewById(R.id.video_overlay).performClick();

        assertThat(adView.findViewById(R.id.video_pause_control)).isNotVisible();
    }

    @Test
    public void clickThroughOnCallToActionClick() {
        adView.findViewById(R.id.cta_button).performClick();

        verify(pageListener).onClickThrough();
    }

    @Test
    public void clickSkipAdShouldSkipAd() {
        adView.findViewById(R.id.skip_ad).performClick();

        verify(pageListener).onSkipAd();
    }

    @Test
    public void clickFullscreenShouldFullscreen() {
        adView.findViewById(R.id.video_fullscreen_control).performClick();

        verify(pageListener).onFullscreen();
    }

    @Test
    public void clickShrinkShouldShrink() {
        adView.findViewById(R.id.video_shrink_control).performClick();

        verify(pageListener).onShrink();
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

    private Iterable<View> fadingViews() {
        VideoAdPresenter.Holder holder = (VideoAdPresenter.Holder) adView.getTag();
        return holder.fadingViews;
    }

    private Iterable<View> fadingViewsWithPause() {
        VideoAdPresenter.Holder holder = (VideoAdPresenter.Holder) adView.getTag();
        return holder.fadingViewsWithPause;
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return new PlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration));
    }

    private Player.StateTransition createStateTransition(PlayerState state, Player.Reason reason) {
        return new Player.StateTransition(state, reason, Urn.forTrack(123L));
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

    private VideoAd buildAd(boolean isVertical) {
        final int width = isVertical ? VERTICAL_VIDEO_WIDTH : LETTERBOX_VIDEO_WIDTH;
        final int height = isVertical ? VERTICAL_VIDEO_HEIGHT : LETTERBOX_VIDEO_HEIGHT;
        final VideoAd ad = AdFixtures.getVideoAd(Urn.forTrack(123L), ApiVideoSource.create("codec", "url", 1000, width, height));
        ad.setMonetizableCreator("Artist");
        ad.setMonetizableTitle("Title");
        return ad;
    }

    private void bindVerticalVideo() {
        adView.findViewById(R.id.video_container).layout(0, 0, VERTICAL_VIDEO_WIDTH, VERTICAL_VIDEO_HEIGHT);
        presenter.bindItemView(adView, new VideoPlayerAd(buildAd(true), PropertySet.create()));
    }

    private void bindLetterboxVideo() {
        adView.findViewById(R.id.video_container).layout(0, 0, LETTERBOX_VIDEO_WIDTH, LETTERBOX_VIDEO_HEIGHT);
        presenter.bindItemView(adView, new VideoPlayerAd(buildAd(false), PropertySet.create()));
    }
}
