package com.soundcloud.android.playback.ui;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.ApiVideoSource;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.utils.DeviceHelper;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class VideoAdPresenterTest extends AndroidUnitTest {

    private VideoAdPresenter presenter;
    private View adView;

    private static final int VERTICAL_VIDEO_WIDTH = 600;
    private static final int VERTICAL_VIDEO_HEIGHT = 1024;
    private static final int LETTERBOX_VIDEO_WIDTH = 250;
    private static final int LETTERBOX_VIDEO_HEIGHT = 100;

    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;
    @Mock private DeviceHelper deviceHelper;

    @Before
    public void setUp() throws Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(mock(PlayerOverlayController.class));
        when(deviceHelper.isOrientation(ORIENTATION_PORTRAIT)).thenReturn(true);

        presenter = new VideoAdPresenter(imageOperations,
                                         pageListener,
                                         playerOverlayControllerFactory,
                                         deviceHelper,
                                         resources());
        adView = presenter.createItemView(new FrameLayout(context()), null);
        bindVerticalVideo(true);
    }

    @Test
    public void videoViewNotVisibleOnVerticalVideoBind() {
        bindVerticalVideo(true);

        assertThat(adView.findViewById(R.id.video_view)).isNotVisible();
    }

    @Test
    public void letterboxVideoBindShowsLoadingIndicatorAndLetterboxBackgroundButNotVideoView() {
        bindLetterboxVideo(true);

        assertThat(adView.findViewById(R.id.video_view)).isNotVisible();
        assertThat(adView.findViewById(R.id.letterbox_background)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }


    @Test
    public void letterboxVideoBindDoesntShowLoadingIndicatorAndLetterboxBackgroundButNotVideoViewWithInactivePlaySession() {
        bindLetterboxVideo(false);

        assertThat(adView.findViewById(R.id.video_view)).isNotVisible();
        assertThat(adView.findViewById(R.id.letterbox_background)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void loadingIndicatorVisibleOnVerticalVideoBind() {
        bindVerticalVideo(true);

        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void loadingIndicatorNotVisibleOnVerticalVideoBindWithInactivePlaySession() {
        bindVerticalVideo(false);

        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void videoViewShouldBeVisibleAndLoadingIndicatorGoneAfterPlaybackStarts() {
        bindLetterboxVideo(true);

        presenter.setPlayState(adView, TestPlayStates.playing(), true, true);
        assertThat(adView.findViewById(R.id.video_view)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void loadingIndicatorAndVideoViewAreVisibleOnPlaybackBuffering() {
        bindLetterboxVideo(true);

        presenter.setPlayState(adView, TestPlayStates.playing(), true, true);
        presenter.setPlayState(adView, TestPlayStates.buffering(), true, true);

        assertThat(adView.findViewById(R.id.video_view)).isVisible();
        assertThat(adView.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void loadingIndicatorIsntVisibleWhenPlaybackPaused() {
        bindLetterboxVideo(true);

        presenter.setPlayState(adView, TestPlayStates.idle(), true, true);

        assertThat(adView.findViewById(R.id.video_progress)).isNotVisible();
    }

    @Test
    public void videoViewAspectRatioMaintainedForLetterbox() {
        bindLetterboxVideo(true);
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
        bindLetterboxVideo(true);

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
        final ViewGroup.LayoutParams layoutParams = adView.findViewById(R.id.video_view).getLayoutParams();
        assertThat(layoutParams.width).isGreaterThanOrEqualTo(resources().getDisplayMetrics().widthPixels);
        assertThat(layoutParams.height).isGreaterThanOrEqualTo(resources().getDisplayMetrics().heightPixels);
    }

    @Test
    public void videoOverlayContainerResizedToVideoInPortrait() {
        bindLetterboxVideo(true);
        final ViewGroup.LayoutParams videoAdParams = adView.findViewById(R.id.video_view).getLayoutParams();
        final ViewGroup.LayoutParams videoOverlayLayoutParams = adView.findViewById(R.id.video_overlay_container)
                                                                      .getLayoutParams();

        assertThat(videoOverlayLayoutParams.height).isEqualTo(videoAdParams.height);
        assertThat(videoOverlayLayoutParams.width).isEqualTo(videoAdParams.width);
    }

    @Test
    public void getVideoTextureReturnsTextureView() {
        assertThat(presenter.getVideoTexture(adView)).isEqualTo(adView.findViewById(R.id.video_view));
    }

    @Test
    public void fadingViewsAreInvisibleAfterPlaybackStarts() {
        presenter.setPlayState(adView, TestPlayStates.playing(), true, true);

        for (View view : fadingViews()) {
            assertThat(view).isInvisible();
        }
    }

    @Test
    public void fadingViewsAreNotInvisibleAfterPlaybackStartsForNonCurrentItem() {
        presenter.setPlayState(adView, TestPlayStates.playing(), false, true);

        for (View view : fadingViews()) {
            assertThat(view).isNotInvisible();
        }
    }

    @Test
    public void fadingViewsAreVisibleOnPlaybackPause() {
        presenter.setPlayState(adView, TestPlayStates.idle(), true, true);

        for (View view : fadingViews()) {
            assertThat(view).isVisible();
        }
    }

    @Test
    public void fadingViewsAreSetToInvisibleAfterPlayEventFromPause() {
        presenter.setPlayState(adView, TestPlayStates.idle(), true, true);
        presenter.setPlayState(adView, TestPlayStates.playing(), true, true);

        for (View view : fadingViews()) {
            assertThat(view).isInvisible();
        }
    }

    @Test
    public void togglePlayOnPlayClick() {
        adView.findViewById(R.id.player_play).performClick();

    }

    @Test
    public void togglePlayOnVideoOverlayClick() {
        adView.findViewById(R.id.video_overlay).performClick();

        verify(pageListener).onTogglePlay();
    }

    @Test
    public void clickThroughOnCallToActionClick() {
        final View view = adView.findViewById(R.id.cta_button);
        view.performClick();

        verify(pageListener).onClickThrough(view.getContext());
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

    private Iterable<View> fadingViews() {
        VideoAdPresenter.Holder holder = (VideoAdPresenter.Holder) adView.getTag();
        return holder.fadingViews;
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return TestPlaybackProgress.getPlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration));
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
        final VideoAd ad = AdFixtures.getVideoAd(Urn.forTrack(123L),
                                                 ApiVideoSource.create("codec", "url", 1000, width, height));
        ad.setMonetizableCreator("Artist");
        ad.setMonetizableTitle("Title");
        return ad;
    }

    private void bindVerticalVideo(boolean playSessionActive) {
        adView.findViewById(R.id.video_container).layout(0, 0, VERTICAL_VIDEO_WIDTH, VERTICAL_VIDEO_HEIGHT);
        adView.findViewById(R.id.play_controls).setVisibility(playSessionActive ? View.GONE : View.VISIBLE);
        presenter.bindItemView(adView, new VideoPlayerAd(buildAd(true)));
    }

    private void bindLetterboxVideo(boolean playSessionActive) {
        adView.findViewById(R.id.video_container).layout(0, 0, LETTERBOX_VIDEO_WIDTH, LETTERBOX_VIDEO_HEIGHT);
        adView.findViewById(R.id.play_controls).setVisibility(playSessionActive ? View.GONE : View.VISIBLE);
        presenter.bindItemView(adView, new VideoPlayerAd(buildAd(false)));
    }

    private void bindUnskippableAd() {
        final VideoAd videoAd = AdFixtures.getNonskippableVideoAd(Urn.forTrack(123L));
        presenter.bindItemView(adView, new VideoPlayerAd(videoAd));
    }
}
