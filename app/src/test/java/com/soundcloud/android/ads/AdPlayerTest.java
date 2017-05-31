package com.soundcloud.android.ads;

import static com.soundcloud.android.events.AdPlaybackEvent.AdProgressEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.AdPlaybackEvent.AdPlayStateTransition;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoAdPlaybackItem;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Date;

public class AdPlayerTest extends AndroidUnitTest {
    private static final VideoAd VIDEO_AD = AdFixtures.getInlayVideoAd(1L);
    private static final VideoAdPlaybackItem VIDEO_ITEM = VideoAdPlaybackItem.create(VIDEO_AD, 0L, 0L);
    private static final boolean NOT_USER_INITIATED = false;
    private static final boolean USER_INITIATED = true;

    @Mock MediaPlayerAdapter adapter;
    @Mock CurrentDateProvider currentDateProvider;
    @Mock PlaySessionController playSessionController;
    @Mock AdAnalyticsController analyticsController;
    @Mock AdStateProvider stateProvider;
    @Mock AdViewabilityController adViewabilityController;

    private TestEventBus eventBus;
    private AdPlayer player;

    @Before
    public void setUp() {
        when(currentDateProvider.getCurrentDate()).thenReturn(new Date(999));
        eventBus = new TestEventBus();
        player = new AdPlayer(adapter, eventBus, adViewabilityController, analyticsController,
                              stateProvider, playSessionController, currentDateProvider);
    }

    @Test
    public void getCurrentAdReturnsAd() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        assertThat(player.getCurrentAd()).isEqualTo(Optional.of(VIDEO_AD));
    }

    @Test
    public void getCurrentAdReturnsAbsentIfNoAdExists() {
        assertThat(player.getCurrentAd()).isEqualTo(Optional.absent());
    }

    @Test
    public void autoplayWhenNoAdWasPlayedBeforeShouldStopForTrackTransitionAndPlay() {
        player.autoplay(VIDEO_AD);

        verify(adapter).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void autoplayWhenUserInitiatedPauseWillNotResumeAd() {
        player.play(VIDEO_AD, false);
        player.pause();

        player.autoplay(VIDEO_AD);

        verify(adapter, never()).resume(any(PlaybackItem.class));
    }

    @Test
    public void autoplayWhenUserDidNotInitiatePauseWillResumeAd() {
        player.play(VIDEO_AD, false);
        player.onPlaystateChanged(TestPlayerTransitions.idle(VIDEO_AD.adUrn(), -1, -1, PlayStateReason.NONE));

        player.autoplay(VIDEO_AD);

        verify(adapter).resume(VIDEO_ITEM);
    }

    @Test
    public void autoplayWhenAdIsNewAdShouldPlayNewAd() {
        final VideoAd ad = AdFixtures.getVideoAd(Urn.forAd("123", "ABC"), Urn.forTrack(123));
        final VideoAdPlaybackItem item = VideoAdPlaybackItem.create(ad, 0L, 1L);

        player.play(VIDEO_AD, false);
        player.autoplay(ad);

        verify(adapter, times(2)).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
        verify(adapter).play(item);
    }

    @Test
    public void videoIsInitiallyMutedForInlayAds() {
        final VideoAd ad = AdFixtures.getInlayVideoAd(1L);
        player.play(ad, false);
        final VideoAdPlaybackItem item = VideoAdPlaybackItem.create(ad, 0L, 0L);

        verify(adapter).play(item);
    }

    @Test
    public void videoIsNotMutedForSponsoredSessions() {
        final VideoAd ad = AdFixtures.sponsoredSessionAd().video();
        player.play(ad, false);
        final VideoAdPlaybackItem item = VideoAdPlaybackItem.create(ad, 0L, 1L);

        verify(adapter).play(item);
    }

    @Test
    public void playForwardsPlayToMediaPlayerAdapter() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        verify(adapter).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void playCallsResumeOnMediaPlayerAdapterIfSameItemAndIsPaused() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(TestPlayerTransitions.idle(VIDEO_ITEM.getUrn()));
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).resume(VIDEO_ITEM);
    }

    @Test
    public void playPausesPlaySessionVideoUnMutedAndIfMusicPlayingBeforeResuming() {
        when(playSessionController.isPlaying()).thenReturn(false, true);

        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        player.onPlaystateChanged(TestPlayerTransitions.idle(VIDEO_ITEM.getUrn()));
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        verify(playSessionController).pause();
    }

    @Test
    public void playCallsPlayOnMediaPlayerAdapterIfDifferentItemPlaying() {
        player.onPlaystateChanged(TestPlayerTransitions.playing());

        player.play(VIDEO_AD, NOT_USER_INITIATED);

        verify(adapter).stopForTrackTransition();
        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void pauseForwardsPauseCallToMediaPlayerAdapter() {
        player.pause();

        verify(adapter).pause();
    }

    @Test
    public void isPlayingReturnsTrueIfLastStateWasPlaying() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(TestPlayerTransitions.playing());

        assertThat(player.isPlaying()).isTrue();
    }

    @Test
    public void isPlayingReturnFalseIfLastStateWasIdle() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(TestPlayerTransitions.idle());

        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void isPlayingReturnFalseIfLastStateWasBuffering() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(TestPlayerTransitions.buffering());

        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void toggleVolumeAfterPlayUnmutes() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();

        verify(adapter).setVolume(1.0f);
    }

    @Test
    public void toggleVolumeAfterPlayUnmutesEmitsPlayStateChange() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();

        assertThat(eventBus.lastEventOn(EventQueue.AD_PLAYBACK)).isInstanceOf(AdPlayStateTransition.class);
    }

    @Test
    public void toggleVolumeAfterPlayUnmutesAndPausesPlaysessionIfMusicIsPlaying() {
        when(playSessionController.isPlaying()).thenReturn(true);

        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();

        verify(adapter).setVolume(1.0f);
        verify(playSessionController).pause();
    }

    @Test
    public void toggleVolumeWhenUnmutedWillMuteAudio() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        player.toggleVolume();

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).setVolume(1.0f);
        inOrder.verify(adapter).setVolume(0.0f);
    }

    @Test
    public void autoPauseWithMuteWillMutePlayerBeforePausingIfNotAlreadyMuted() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        player.autopause(true);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).setVolume(1.0f);
        inOrder.verify(adapter).setVolume(0.0f);
        inOrder.verify(adapter).pause();
    }

    @Test
    public void autoPauseWithNoMuteWillJustPause() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        player.autopause(false);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).setVolume(1.0f);
        inOrder.verify(adapter, never()).setVolume(0.0f);
        inOrder.verify(adapter).pause();
    }

    @Test
    public void mutesVolumeIfMusicStartedAndVideoIsPlayingUnmuted() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).setVolume(1.0f);
        inOrder.verify(adapter).setVolume(0.0f);
    }

    @Test
    public void emitsEventsWhenVideoUnmuted() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();

        verify(adViewabilityController).onVolumeToggle(VIDEO_AD.uuid(), false);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo("VIDEO_AD_UNMUTE");
    }

    @Test
    public void emitsEventsWhenVideoMuted() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.toggleVolume();
        player.toggleVolume();

        final InOrder inOrder = Mockito.inOrder(adViewabilityController);
        inOrder.verify(adViewabilityController).onVolumeToggle(VIDEO_AD.uuid(), false);
        inOrder.verify(adViewabilityController).onVolumeToggle(VIDEO_AD.uuid(), true);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo("VIDEO_AD_MUTE");
    }

    @Test
    public void togglePlayWillPauseAndResumeVideoIfAlreadyPlaying() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        player.onPlaystateChanged(TestPlayerTransitions.playing(VIDEO_AD.adUrn()));
        player.togglePlayback(VIDEO_AD);
        player.onPlaystateChanged(TestPlayerTransitions.idle(VIDEO_AD.adUrn()));
        player.togglePlayback(VIDEO_AD);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).pause();
        inOrder.verify(adapter).resume(VIDEO_ITEM);
    }

    @Test
    public void togglePlayWillPlayNonAlreadyPlayingTrack() {
        player.togglePlayback(VIDEO_AD);

        verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void togglePlaybackWillRestartFinishedAd() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(TestPlayerTransitions.complete(VIDEO_AD.adUrn()));
        player.togglePlayback(VIDEO_AD);

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).play(VIDEO_ITEM);
        inOrder.verify(adapter).play(VIDEO_ITEM);
    }

    @Test
    public void userInitiatedStateChangesAreForwardedToAnalyticsController() {
        ArgumentCaptor<PlayStateEvent> requestData = ArgumentCaptor.forClass(PlayStateEvent.class);
        final PlaybackStateTransition transition = TestPlayerTransitions.playing(VIDEO_AD.adUrn());

        player.play(VIDEO_AD, USER_INITIATED);
        player.onPlaystateChanged(transition);

        verify(analyticsController).onStateTransition(eq(Screen.STREAM), eq(USER_INITIATED), eq(VIDEO_AD), requestData.capture());
        assertThat(requestData.getValue().getTransition()).isEqualTo(transition);
    }

    @Test
    public void automaticStateChangesAreForwardedToAnalyticsController() {
        ArgumentCaptor<PlayStateEvent> requestData = ArgumentCaptor.forClass(PlayStateEvent.class);
        final PlaybackStateTransition transition = TestPlayerTransitions.playing(VIDEO_AD.adUrn());

        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onPlaystateChanged(transition);

        verify(analyticsController).onStateTransition(eq(Screen.STREAM), eq(NOT_USER_INITIATED), eq(VIDEO_AD), requestData.capture());
        assertThat(requestData.getValue().getTransition()).isEqualTo(transition);
    }

    @Test
    public void stateChangesAreForwardedToInlayStateProviderController() {
        final ArgumentCaptor<AdPlayStateTransition> inlayTransition = ArgumentCaptor.forClass(AdPlayStateTransition.class);
        final PlaybackStateTransition playerTransition = TestPlayerTransitions.playing(VIDEO_AD.adUrn());

        this.player.play(VIDEO_AD, NOT_USER_INITIATED);
        this.player.onPlaystateChanged(playerTransition);

        verify(stateProvider).put(eq(VIDEO_AD.uuid()), inlayTransition.capture());
        assertThat(inlayTransition.getValue().stateTransition()).isEqualTo(playerTransition);
    }

    @Test
    public void stateChangesArePublishedToEventQueue() {
        final PlaybackStateTransition playerTransition = TestPlayerTransitions.playing(VIDEO_AD.adUrn());

        this.player.play(VIDEO_AD, NOT_USER_INITIATED);
        this.player.onPlaystateChanged(playerTransition);

        final AdPlayStateTransition event = (AdPlayStateTransition) eventBus.lastEventOn(EventQueue.AD_PLAYBACK);
        assertThat(event.forStateTransition()).isTrue();
        assertThat(event.stateTransition()).isEqualTo(playerTransition);
    }

    @Test
    public void progressUpdatesAreForwardedToAnalyticsController() {
        ArgumentCaptor<PlaybackProgressEvent> requestData = ArgumentCaptor.forClass(PlaybackProgressEvent.class);

        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onProgressEvent(100, 200);

        verify(analyticsController).onProgressEvent(eq(VIDEO_AD), requestData.capture());
        final PlaybackProgress playbackProgress = requestData.getValue().getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(100);
        assertThat(playbackProgress.getDuration()).isEqualTo(200);
    }

    @Test
    public void progressIsPublishedToEventQueue() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);

        player.onProgressEvent(100, 500);

        final AdProgressEvent event = (AdProgressEvent) eventBus.lastEventOn(EventQueue.AD_PLAYBACK);
        assertThat(event.forAdProgressEvent()).isTrue();
        assertThat(event.playbackProgress().getPosition()).isEqualTo(100);
        assertThat(event.playbackProgress().getDuration()).isEqualTo(500);
    }

    @Test
    public void getLastProgressReturnsProgressEventIfForCurrentlyPlayingAd() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onProgressEvent(100, 200);

        final Optional<PlaybackProgress> playbackProgress = player.lastPosition(VIDEO_AD);
        assertThat(playbackProgress.isPresent()).isTrue();
        assertThat(playbackProgress.get().getUrn()).isEqualTo(VIDEO_AD.adUrn());
        assertThat(playbackProgress.get().getPosition()).isEqualTo(100);
        assertThat(playbackProgress.get().getDuration()).isEqualTo(200);
    }

    @Test
    public void getLastProgressReturnsAbsentIfNotForCurrentlyPlayingAd() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.onProgressEvent(100, 200);

        assertThat(player.lastPosition(AdFixtures.getVideoAd(Urn.forAd("1", "2"), Urn.forTrack(123))).isPresent()).isFalse();
    }

    @Test
    public void resetDestroysMediaPlayerAndResetsCurrentAd() {
        player.play(VIDEO_AD, NOT_USER_INITIATED);
        player.reset();

        verify(adapter).destroy();
        assertThat(player.getCurrentAd()).isEqualTo(Optional.absent());
    }
}
