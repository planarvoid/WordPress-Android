package com.soundcloud.android.ads;

import static com.soundcloud.android.events.InlayAdEvent.InlayPlayStateTransition;
import static com.soundcloud.android.events.InlayAdEvent.TogglePlayback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import java.util.Date;

public class FullScreenVideoPresenterTest extends AndroidUnitTest {

    private final static VideoAd VIDEO_AD = AdFixtures.getVideoAd(Urn.forTrack(123L));

    @Mock FullScreenVideoView videoView;
    @Mock InlayAdPlayer inlayAdPlayer;
    @Mock InlayAdStateProvider stateProvider;
    @Mock CurrentDateProvider dateProvider;
    @Mock StreamAdsController controller;
    @Mock Navigator navigator;

    @Mock AppCompatActivity activity;

    private TestEventBus eventBus;
    private FullScreenVideoPresenter presenter;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        presenter = new FullScreenVideoPresenter(videoView, inlayAdPlayer, stateProvider, controller, dateProvider, navigator, eventBus);

        final Intent intent = new Intent(context(), FullScreenVideoActivity.class);
        intent.putExtra(FullScreenVideoActivity.EXTRA_AD_URN, VIDEO_AD.getAdUrn());

        when(activity.getIntent()).thenReturn(intent);
        when(inlayAdPlayer.getCurrentAd()).thenReturn(Optional.of(VIDEO_AD));
        when(stateProvider.get(VIDEO_AD.getUuid())).thenReturn(Optional.absent());
        when(dateProvider.getCurrentDate()).thenReturn(new Date(999));
    }

    @Test
    public void onVideoShrinkCallsFinishOnActivity() {
        presenter.onCreate(activity, null);
        presenter.onShrinkClick();

        verify(activity).finish();
    }

    @Test
    public void onLearnMoreClickOpensClickthroughAndSendsTrackingEvent() {
        presenter.onCreate(activity, null);
        presenter.onLearnMoreClick(context());

        final Uri clickThroughUrl = Uri.parse(VIDEO_AD.getClickThroughUrl());
        verify(navigator).openAdClickthrough(context(), clickThroughUrl);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(2);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void onTogglePlaySendsTogglePlayInlayAdEvent() {
        presenter.onCreate(activity, null);
        presenter.onTogglePlayClick();

        assertThat(eventBus.lastEventOn(EventQueue.INLAY_AD)).isInstanceOf(TogglePlayback.class);
    }

    @Test
    public void onResumeListensToTransitionEventsAndForwardsThemToView() {
        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        final PlaybackStateTransition transition = TestPlayerTransitions.playing();
        final InlayPlayStateTransition event = InlayPlayStateTransition.create(VIDEO_AD, transition, false, new Date(999));
        eventBus.publish(EventQueue.INLAY_AD, event);

        verify(videoView).setPlayState(transition);
    }

    @Test
    public void onResumeRebindsVideoTextureView() {
        presenter.onCreate(activity, null);
        presenter.onResume(activity);

        verify(videoView).bindVideoSurface(VIDEO_AD.getUuid(), VideoSurfaceProvider.Origin.FULLSCREEN);
    }

    @Test
    public void onDestroyUnbindsVideoTextureViewAndSendsTrackingEvent() {
        presenter.onCreate(activity, null);
        presenter.onDestroy(activity);

        verify(videoView).unbindVideoSurface( VideoSurfaceProvider.Origin.FULLSCREEN);
        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(2);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void onCreateSendsUIEventTrackingEventForFullscreen() {
        presenter.onCreate(activity, null);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(UIEvent.class);
    }

    @Test
    public void onCreateSetsScreenSizeChangeTrueOnStreamAdsController() {
        presenter.onCreate(activity, null);

        verify(controller).onScreenSizeChange(true);
    }

    @Test
    public void onPauseSetsScreenSizeChangeFalseOnStreamAdsController() {
        presenter.onCreate(activity, null);
        presenter.onPause(activity);

        verify(controller).onScreenSizeChange(false);
    }

    @Test
    public void onCreateForAdWillSetupContentOnView() {
        presenter.onCreate(activity, null);

        verify(videoView).setupContentView(activity, VIDEO_AD);
    }

    @Test
    public void onCreateForNoAdWillFinishActivity() {
        when(inlayAdPlayer.getCurrentAd()).thenReturn(Optional.absent());
        presenter.onCreate(activity, null);

        verify(videoView, never()).setupContentView(activity, VIDEO_AD);
        verify(activity).finish();
    }

    @Test
    public void onCreateForOtherAdWillFinishActivity() {
        when(inlayAdPlayer.getCurrentAd()).thenReturn(Optional.of(AdFixtures.getVideoAd(Urn.forAd("321", "abc"), Urn.forTrack(123L))));
        presenter.onCreate(activity, null);

        verify(videoView, never()).setupContentView(activity, VIDEO_AD);
        verify(activity).finish();
    }

    @Test
    public void onCreateForwardsLastStateIfPlaybackIsContinuingForVideoToView() {
        final PlaybackStateTransition transition = TestPlayerTransitions.playing();
        final InlayPlayStateTransition event = InlayPlayStateTransition.create(VIDEO_AD, transition, false, new Date(999));
        when(stateProvider.get(VIDEO_AD.getUuid())).thenReturn(Optional.of(event));

        presenter.onCreate(activity, null);

        verify(videoView).setPlayState(transition);
    }

    @Test
    public void onCreateFinishesActivityIfLastStateForVideoIsError() {
        final PlaybackStateTransition transition = TestPlayerTransitions.error(PlayStateReason.ERROR_FAILED);
        final InlayPlayStateTransition event = InlayPlayStateTransition.create(VIDEO_AD, transition, false, new Date(999));
        when(stateProvider.get(VIDEO_AD.getUuid())).thenReturn(Optional.of(event));

        presenter.onCreate(activity, null);

        verify(videoView, never()).setPlayState(transition);
        verify(activity).finish();
    }
}