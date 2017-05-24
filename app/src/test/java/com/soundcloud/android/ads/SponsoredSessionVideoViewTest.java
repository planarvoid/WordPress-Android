package com.soundcloud.android.ads;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Date;

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

    public AdPlayStateTransition createTransition(VideoAd ad, PlaybackStateTransition transition) {
        return AdPlayStateTransition.create(ad, transition, false, new Date(1L));
    }

}