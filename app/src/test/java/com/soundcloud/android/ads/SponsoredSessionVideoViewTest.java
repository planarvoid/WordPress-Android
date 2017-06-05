package com.soundcloud.android.ads;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlaybackProgress;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SponsoredSessionVideoViewTest extends AndroidUnitTest {

    private final static SponsoredSessionAd SPONSORED_SESSION = AdFixtures.sponsoredSessionAd();

    @Mock AdStateProvider adStateProvider;
    @Mock PrestitialView.Listener listener;

    private View view;
    private SponsoredSessionVideoView videoView;

    @Before
    public void setUp() {
        view = LayoutInflater.from(context()).inflate(R.layout.sponsored_session_video_page, new FrameLayout(context()));
        videoView = spy(new SponsoredSessionVideoView(resources(), adStateProvider));
        when(adStateProvider.get(SPONSORED_SESSION.video().uuid())).thenReturn(Optional.absent());
    }

    @Test
    public void onContentSetWillSetupStateIfAdStateProviderReturnsTransition() {
        final AdPlayStateTransition transition = createTransition(SPONSORED_SESSION.video(), TestPlayerTransitions.playing());
        when(adStateProvider.get(SPONSORED_SESSION.video().uuid())).thenReturn(Optional.of(transition));

        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        verify(videoView).setPlayState(transition.stateTransition());
    }

    @Test
    public void onContentSetWillNotSetupStateIfAdStateProviderReturnsNothing() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        verify(videoView, never()).setPlayState(any(PlaybackStateTransition.class));
    }

    @Test
    public void playButtonIsVisibleIfPlayerIsPaused() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.idle());

        assertThat(view.findViewById(R.id.player_play)).isVisible();
    }

    @Test
    public void playOverlayIsVisibleIfPlayerIsPaused() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.idle());

        assertThat(view.findViewById(R.id.video_overlay)).isVisible();
    }

    @Test
    public void playButtonIsGoneIfPlayerIsPlaying() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.playing());

        assertThat(view.findViewById(R.id.player_play)).isGone();
    }

    @Test
    public void playOverlayIsGoneIfPlayerIsPlaying() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.playing());

        assertThat(view.findViewById(R.id.video_overlay)).isGone();
    }

    @Test
    public void videoViewIsVisibleIfPlayerIsPlaying() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.playing());

        assertThat(view.findViewById(R.id.video_view)).isVisible();
    }

    @Test
    public void loadingIndicatorIsVisibleIfPlayerIsBuffering() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        videoView.setPlayState(TestPlayerTransitions.buffering());

        assertThat(view.findViewById(R.id.video_progress)).isVisible();
    }

    @Test
    public void loadingIndicatorIsGoneIfPlayerIsntBuffering() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setPlayState(TestPlayerTransitions.idle());
        assertThat(view.findViewById(R.id.video_progress)).isGone();

        videoView.setPlayState(TestPlayerTransitions.playing());
        assertThat(view.findViewById(R.id.video_progress)).isGone();

        videoView.setPlayState(TestPlayerTransitions.complete());
        assertThat(view.findViewById(R.id.video_progress)).isGone();
    }

    @Test
    public void pressingPlayButtonCallsTogglePlaybackOnListener() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        view.findViewById(R.id.player_play).performClick();

        verify(listener).onTogglePlayback();
    }

    @Test
    public void pressingVideoViewCallsTogglePlaybackOnListener() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        view.findViewById(R.id.video_view).performClick();

        verify(listener).onTogglePlayback();
    }

    @Test
    public void pressingVideoOverlayCallsTogglePlaybackOnListener() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        view.findViewById(R.id.video_overlay).performClick();

        verify(listener).onTogglePlayback();
    }

    @Test
    public void pressingWhyAdsTextCallsOnWhyAdsOnListener() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);
        final View whyAdsView = view.findViewById(R.id.why_ads);

        whyAdsView.performClick();

        verify(listener).onWhyAdsClicked(whyAdsView.getContext());
    }

    @Test
    public void setProgressShouldInitiallySetATimerTo1minute() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 0, 100));

        assertThat(videoView.skipAd).isGone();
        assertThat(videoView.timeUntilSkip).isVisible();
        assertThat(videoView.timeUntilSkip).containsText("Skip in: 1 min.");
    }

    @Test
    public void setProgressShouldUpdateTimeUntilSkipAccordingToPosition() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 7, 100));

        assertThat(videoView.skipAd).isGone();
        assertThat(videoView.timeUntilSkip).isVisible();
        assertThat(videoView.timeUntilSkip).containsText("Skip in: 53 sec.");
    }

    @Test
    public void setProgressShouldDisplayEnableSkipAdAfter1minute() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 60, 100));

        assertThat(videoView.timeUntilSkip).isGone();
        assertThat(videoView.skipAd).isVisible();
    }

    @Test
    public void skipAdButtonShouldCallOnSkipAdInListener() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 60, 100));

        videoView.skipAd.performClick();

        verify(listener).onSkipAd();
    }


    @Test
    public void setProgressForAdsLessThan1minuteShouldInitiallySetTimerToDuration() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 0, 30));

        assertThat(videoView.skipAd).isGone();
        assertThat(videoView.timeUntilSkip).isVisible();
        assertThat(videoView.timeUntilSkip).containsText("30 sec.");
    }

    @Test
    public void setProgressForAdsLessThan1minuteShouldUpdateTimerToDuration() {
        videoView.setupContentView(view, SPONSORED_SESSION, listener);

        videoView.setProgress(createProgress(TimeUnit.SECONDS, 17, 30));

        assertThat(videoView.skipAd).isGone();
        assertThat(videoView.timeUntilSkip).isVisible();
        assertThat(videoView.timeUntilSkip).containsText("13 sec.");
    }

    public AdPlayStateTransition createTransition(VideoAd ad, PlaybackStateTransition transition) {
        return AdPlayStateTransition.create(ad, transition, false, new Date(1L));
    }

    private PlaybackProgress createProgress(TimeUnit timeUnit, int position, int duration) {
        return TestPlaybackProgress.getPlaybackProgress(timeUnit.toMillis(position), timeUnit.toMillis(duration));
    }
}
