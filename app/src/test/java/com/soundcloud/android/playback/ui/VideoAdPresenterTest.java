package com.soundcloud.android.playback.ui;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.Player.PlayerState;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerVideoAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoAdPresenterTest extends AndroidUnitTest {

    private VideoAdPresenter presenter;
    private View adView;

    @Mock private MediaPlayerVideoAdapter mediaPlayerVideoAdapter;
    @Mock private ImageOperations imageOperations;
    @Mock private PlayerOverlayController playerOverlayController;
    @Mock private AdPageListener pageListener;
    @Mock private PlayerOverlayController.Factory playerOverlayControllerFactory;

    @Before
    public void setUp() throws  Exception {
        when(playerOverlayControllerFactory.create(any(View.class))).thenReturn(mock(PlayerOverlayController.class));

        presenter = new VideoAdPresenter(mediaPlayerVideoAdapter, imageOperations, pageListener, playerOverlayControllerFactory, resources());
        adView = presenter.createItemView(new FrameLayout(context()), null);
        presenter.bindItemView(adView, new PlayerAd(buildAd(), PropertySet.create()));
    }

    @Test
    public void fadeableViewsWithoutPauseButtonVisibleOnBind() {
        for (View view : fadeableViews()) {
            assertThat(view).isVisible();
        }
        assertThat(adView.findViewById(R.id.video_pause_control)).isNotVisible();
    }

    @Test
    public void fadeableViewsWithoutPauseButtonVisibleOnClear() {
        presenter.clearItemView(adView);

        for (View view : fadeableViews()) {
            assertThat(view).isVisible();
        }
        assertThat(adView.findViewById(R.id.video_pause_control)).isNotVisible();
    }

    @Test
    public void fadeableViewsAreInvisibleAfterPlaybackStarts() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);

        for (View view : fadeableViews()) {
            assertThat(view).isInvisible();
        }
    }

    @Test
    public void fadeableViewsAreNotInvisibleAfterPlaybackStartsForNonCurrentItem() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), false, true);

        for (View view : fadeableViews()) {
            assertThat(view).isNotInvisible();
        }
    }

    @Test
    public void fadeableViewsAreVisibleOnPlaybackPause() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);

        for (View view : fadeableViews()) {
            assertThat(view).isVisible();
        }
    }

    @Test
    public void fadeableViewsWithPauseAreSetToInvisibleAfterPlayEventFromPause() {
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.IDLE, Player.Reason.NONE), true, true);
        presenter.setPlayState(adView,
                createStateTransition(PlayerState.PLAYING, Player.Reason.NONE), true, true);

        for (View view : fadeableViewsWithPause()) {
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

    private Iterable<View> fadeableViews() {
        VideoAdPresenter.Holder holder = (VideoAdPresenter.Holder) adView.getTag();
        return holder.fadeableUIViews;
    }

    private Iterable<View> fadeableViewsWithPause() {
        VideoAdPresenter.Holder holder = (VideoAdPresenter.Holder) adView.getTag();
        return holder.fadeableUIViewsWithPause;
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

    private PlayerAdData buildAd() {
        final VideoAd ad = AdFixtures.getVideoAd(Urn.forTrack(123L));
        ad.setMonetizableCreator("Artist");
        ad.setMonetizableTitle("Title");
        return ad;
    }
}
