package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.mediasession.MediaSessionController;
import com.soundcloud.android.playback.mediasession.MediaSessionControllerFactory;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.shadows.ShadowLooper;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

public class PlaybackServiceTest extends AndroidUnitTest {

    private static final long START_POSITION = 123L;
    private static final long NORMAL_FADE_DURATION = 600;
    private static final long PREVIEW_FADE_DURATION = 2000;
    public static final TrackItem UPSELLABLE_TRACK = TestPropertySets.upsellableTrack();

    private PlaybackService playbackService;
    private PlaybackItem playbackItem = AudioPlaybackItem.create(TestPropertySets.fromApiTrack(), START_POSITION);
    private PlaybackItem snippetPlaybackItem = AudioPlaybackItem.forSnippet(UPSELLABLE_TRACK, START_POSITION);
    private Urn track = playbackItem.getUrn();
    private TestEventBus eventBus = new TestEventBus();
    private TestDateProvider dateProvider = new TestDateProvider();

    private IntentFilter playbackFilter;

    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamPlayer streamPlayer;
    @Mock private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock private PlaybackReceiver playbackReceiver;
    @Mock private PlayQueue playQueue;
    @Mock private PlaybackStateTransition stateTransition;
    @Mock private PlaybackAnalyticsController analyticsDispatcher;
    @Mock private AdsOperations adsOperations;
    @Mock private VolumeControllerFactory volumeControllerFactory;
    @Mock private MediaSessionControllerFactory mediaSessionControllerFactory;
    @Mock private VolumeController volumeController;
    @Mock private MediaSessionController mediaSessionController;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private PlayStatePublisher playStatePublisher;
    @Captor private ArgumentCaptor<PlaybackProgressEvent> playbackProgressEventCaptor;

    @Before
    public void setUp() throws Exception {
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        playbackService = new PlaybackService(eventBus,
                                              accountOperations,
                                              streamPlayer,
                                              playbackReceiverFactory,
                                              analyticsDispatcher,
                                              volumeControllerFactory,
                                              mediaSessionControllerFactory,
                                              playSessionStateProvider,
                                              playStatePublisher) {
            /**
             * This is a huge hack to not crash the unit tests, as we are not using RL properly, because of the
             * way we do constructor injection of mocks. Hopefully, this does not live too long :-/
             */

            @Override
            void registerPlaybackReceiver(IntentFilter playbackFilter) {
                PlaybackServiceTest.this.playbackFilter = playbackFilter;
            }

            @Override
            void unregisterPlaybackReceiver() {
                PlaybackServiceTest.this.playbackFilter = new IntentFilter();
            }
        };

        when(playbackReceiverFactory.create(playbackService, accountOperations)).thenReturn(playbackReceiver);
        when(volumeControllerFactory.create(streamPlayer, playbackService)).thenReturn(volumeController);
        when(mediaSessionControllerFactory.create(playbackService, playbackService)).thenReturn(mediaSessionController);
    }

    @Test
    public void onCreateSetsServiceAsListenerOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        verify(streamPlayer).setListener(playbackService);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForToggleplaybackAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(PlaybackService.Action.TOGGLE_PLAYBACK)).isTrue();
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPauseAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(PlaybackService.Action.PAUSE)).isTrue();
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForSeek() throws Exception {
        playbackService.onCreate();

        assertThat(playbackFilter.hasAction(PlaybackService.Action.SEEK)).isTrue();
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForResetAllAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(PlaybackService.Action.RESET_ALL)).isTrue();
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForStopAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(PlaybackService.Action.STOP)).isTrue();
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).isTrue();
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForFadeAndPause() throws Exception {
        playbackService.onCreate();
        assertThat(playbackFilter.hasAction(PlaybackService.Action.FADE_AND_PAUSE)).isTrue();
    }

    @Test
    public void onCreatePublishedServiceLifecycleForCreated() throws Exception {
        playbackService.onCreate();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_CREATED);
    }

    @Test
    public void onDestroyPublishedServiceLifecycleForDestroyed() throws Exception {
        playbackService.onCreate();
        playbackService.onDestroy();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_DESTROYED);
    }

    @Test
    public void onDestroyReleasesMediaSession() throws Exception {
        playbackService.onCreate();

        playbackService.onDestroy();

        verify(mediaSessionController).onDestroy();
    }

    @Test
    public void onStartPublishesServiceLifecycleForStarted() throws Exception {
        playbackService.onCreate();
        playbackService.stop();
        playbackService.onStartCommand(new Intent(), 0, 0);

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        assertThat(broadcasted.getKind()).isEqualTo(PlayerLifeCycleEvent.STATE_STARTED);
    }

    @Test
    public void onStartWithNullIntentStopsSelf() throws Exception {
        playbackService.onCreate();
        playbackService.onStartCommand(null, 0, 0);

        assertThat(playbackService).hasStoppedSelf();
    }

    @Test
    public void onStartWithIntentPostsToMediaButtonThroughMediaSession() {
        Intent intent = mock(Intent.class);
        playbackService.onCreate();
        playbackService.stop();

        playbackService.onStartCommand(intent, 0, 0);

        verify(mediaSessionController).onStartCommand(intent);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransitionForVideo() throws Exception {
        final VideoAd videoAd = AdFixtures.getVideoAd(Urn.forTrack(123));
        playbackItem = VideoAdPlaybackItem.create(videoAd, 0L);
        playbackService.onCreate();
        playbackService.play(playbackItem);

        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                                    PlayStateReason.NONE,
                                                                                    videoAd.getAdUrn(),
                                                                                    0,
                                                                                    123,
                                                                                    dateProvider);

        playbackService.onPlaystateChanged(stateTransition);

        verify(playStatePublisher).publish(stateTransition, playbackItem, true);
    }

    @Test
    public void onPlaystateChangedToBufferingItNotifiesMediaSession() {
        playbackService.onCreate();
        playbackService.play(playbackItem);

        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(PlaybackState.BUFFERING,
                                                                                    PlayStateReason.NONE,
                                                                                    track,
                                                                                    123L,
                                                                                    0,
                                                                                    dateProvider);
        playbackService.onPlaystateChanged(stateTransition);

        verify(mediaSessionController).onBuffering(123L);
    }

    @Test
    public void onPlaystateChangedToPlayingItNotifiesMediaSession() {
        playbackService.onCreate();
        playbackService.play(playbackItem);

        final PlaybackStateTransition stateTransition = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                                    PlayStateReason.NONE,
                                                                                    track,
                                                                                    123L,
                                                                                    0,
                                                                                    dateProvider);
        playbackService.onPlaystateChanged(stateTransition);

        verify(mediaSessionController).onPlaying(123L);
    }

    @Test
    public void onProgressForwardsAudioAdProgressEventToAnalyticsDispatcher() {
        playbackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(Urn.forTrack(123L)));
        playbackService.onCreate();
        playbackService.play(playbackItem);
        playbackService.onProgressEvent(25, 50);

        ArgumentCaptor<PlaybackProgressEvent> captor = ArgumentCaptor.forClass(PlaybackProgressEvent.class);
        verify(analyticsDispatcher).onProgressEvent(eq(playbackItem), captor.capture());

        final PlaybackProgressEvent event = captor.getValue();
        assertThat(event.getPlaybackProgress().getPosition()).isEqualTo(25);
        assertThat(event.getPlaybackProgress().getDuration()).isEqualTo(50);
        assertThat(event.getUrn()).isEqualTo(playbackItem.getUrn());
    }

    @Test
    public void onProgressForwardsProgressToMediaSession() {
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(25, 50);

        verify(mediaSessionController).onProgress(25L);
    }

    @Test
    public void onProgressPublishesAProgressEventForTrack() throws Exception {
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(123L, 456L);

        verify(playSessionStateProvider).onProgressEvent(playbackProgressEventCaptor.capture());
        PlaybackProgressEvent broadcasted = playbackProgressEventCaptor.getValue();
        assertThat(broadcasted.getUrn().isTrack()).isTrue();
        assertThat(broadcasted.getUrn()).isEqualTo(track);
        assertThat(broadcasted.getPlaybackProgress().getPosition()).isEqualTo(123L);
        assertThat(broadcasted.getPlaybackProgress().getDuration()).isEqualTo(456L);
    }

    @Test
    public void onProgressPublishesAProgressEventForVideo() throws Exception {
        playbackItem = VideoAdPlaybackItem.create(AdFixtures.getVideoAd(Urn.forTrack(123)), 0L);
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(123L, 456L);

        verify(playSessionStateProvider).onProgressEvent(playbackProgressEventCaptor.capture());
        PlaybackProgressEvent broadcasted = playbackProgressEventCaptor.getValue();
        assertThat(broadcasted.getUrn().isAd()).isTrue();
        assertThat(broadcasted.getUrn()).isEqualTo(Urn.forAd("dfp", "905"));
        assertThat(broadcasted.getPlaybackProgress().getPosition()).isEqualTo(123L);
        assertThat(broadcasted.getPlaybackProgress().getDuration()).isEqualTo(456L);
    }

    @Test
    public void openCurrentWithStreamableTrackCallsPlayOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(mediaSessionController.onPlay(playbackItem)).thenReturn(true);

        playbackService.play(playbackItem);

        verify(streamPlayer).play(playbackItem);
    }

    @Test
    public void doesNotPlayWhenAudioFocusWasNotGained() throws Exception {
        playbackService.onCreate();
        when(mediaSessionController.onPlay(playbackItem)).thenReturn(false);

        playbackService.play(playbackItem);

        verify(streamPlayer, never()).play(playbackItem);
    }

    @Test
    public void stopCallsStopOnStreamPlaya() throws Exception {
        playbackService.onCreate();

        playbackService.stop();

        verify(streamPlayer).stop();
    }

    @Test
    public void stopCallsStopOnMediaSession() throws Exception {
        playbackService.onCreate();

        playbackService.stop();

        verify(mediaSessionController).onStop();
    }


    @Test
    public void callingStopSuppressesIdleNotifications() throws Exception {
        playbackService.onCreate();

        playbackService.play(playbackItem);

        playbackService.stop();
        playbackService.onPlaystateChanged(new PlaybackStateTransition(PlaybackState.IDLE,
                                                                       PlayStateReason.NONE,
                                                                       track, 0, 0));

        assertThat(playbackService).doesNotHaveLastForegroundNotification();
    }

    @Test
    public void onProgressDoesNotFadeNonSnippet() {
        playbackService.onCreate();
        playbackService.play(playbackItem);

        playbackService.onProgressEvent(playbackItem.getDuration() - PREVIEW_FADE_DURATION, playbackItem.getDuration());

        verify(volumeController, never()).fadeOut(2000, 0);
    }

    @Test
    public void onProgressFadesSnippet() {
        playbackService.onCreate();
        playbackService.play(snippetPlaybackItem);

        long position = snippetPlaybackItem.getDuration() - PREVIEW_FADE_DURATION;
        playbackService.onProgressEvent(position, snippetPlaybackItem.getDuration());

        verify(volumeController).fadeOut(2000, 0);
    }

    @Test
    public void onProgressFadesSnippetWithEarlyOffset() {
        playbackService.onCreate();
        playbackService.play(snippetPlaybackItem);

        long position = snippetPlaybackItem.getDuration() - (PREVIEW_FADE_DURATION + 1000);
        playbackService.onProgressEvent(position, snippetPlaybackItem.getDuration());

        verify(volumeController).fadeOut(2000, -1000);
    }

    @Test
    public void onProgressFadesSnippetAfterOffset() {
        playbackService.onCreate();
        playbackService.play(snippetPlaybackItem);

        long position = snippetPlaybackItem.getDuration() - (PREVIEW_FADE_DURATION - 1500);
        playbackService.onProgressEvent(position, snippetPlaybackItem.getDuration());

        verify(volumeController).fadeOut(2000, 1500);
    }

    @Test
    public void onFocusGainUnMutesPlayer() {
        playbackService.onCreate();

        playbackService.onFocusGain();

        verify(volumeController).unMute(NORMAL_FADE_DURATION);
    }

    @Test
    public void onFocusLossVolumeDucksIfItCanDuck() {
        playbackService.onCreate();
        playbackService.play(playbackItem);
        when(streamPlayer.isPlaying()).thenReturn(true);

        playbackService.onFocusLoss(true, true);

        verify(volumeController).duck(NORMAL_FADE_DURATION);
    }

    @Test
    public void onFocusLossVolumeMutesIfItCantDuck() {
        playbackService.onCreate();
        playbackService.play(playbackItem);
        when(streamPlayer.isPlaying()).thenReturn(true);

        playbackService.onFocusLoss(true, false);

        verify(volumeController).mute(NORMAL_FADE_DURATION);
    }

    @Test
    public void onFocusLossVolumeMutesIfItsNotTransient() {
        playbackService.onCreate();
        playbackService.play(playbackItem);
        when(streamPlayer.isPlaying()).thenReturn(true);

        playbackService.onFocusLoss(false, true);

        verify(volumeController).mute(NORMAL_FADE_DURATION);
    }

    @Test
    public void onFocusLossVolumeIsNotMutedIfItIsNotPlaying() {
        playbackService.onCreate();
        playbackService.play(playbackItem);
        when(streamPlayer.isPlaying()).thenReturn(false);

        playbackService.onFocusLoss(false, false);

        verify(volumeController, never()).mute(NORMAL_FADE_DURATION);
    }

    @Test
    public void onSeekVolumeIsReset() {
        playbackService.onCreate();

        playbackService.seek(123L, true);

        verify(volumeController).resetVolume();
    }

    @Test
    public void onSeekForwardsPositionToMediaSession() {
        playbackService.onCreate();

        playbackService.seek(123L, true);

        verify(mediaSessionController).onSeek(123L);
    }

    @Test
    public void onPlayNewTrackVolumeIsReset() {
        when(mediaSessionController.onPlay(playbackItem)).thenReturn(true);
        playbackService.onCreate();

        playbackService.play(playbackItem);

        verify(volumeController).resetVolume();
    }

    @Test
    public void onPlayNewTrackWithPositionFadesIfStartsInTheMiddleOfTheFade() {
        long startPosition = UPSELLABLE_TRACK.getSnippetDuration() - 1000;
        PlaybackItem onFadeOffset = AudioPlaybackItem.forSnippet(UPSELLABLE_TRACK, startPosition);
        when(mediaSessionController.onPlay(onFadeOffset)).thenReturn(true);
        playbackService.onCreate();

        playbackService.play(onFadeOffset);

        InOrder inOrder = Mockito.inOrder(volumeController);
        inOrder.verify(volumeController).resetVolume();
        inOrder.verify(volumeController).fadeOut(2000, 1000);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void onPauseForwardsToMediaSession() {
        playbackService.onCreate();

        playbackService.pause();

        verify(mediaSessionController).onPause();
    }

    @Test
    public void fadeAndPauseFadesSound() {
        playbackService.onCreate();

        playbackService.fadeAndPause();

        verify(volumeController).fadeOut(2000, 0);
    }

    @Test
    public void fadeAndPauseDoesNotPauseUntilFadeFinishes() {
        playbackService.onCreate();

        playbackService.fadeAndPause();

        verify(streamPlayer, never()).pause();
    }

    @Test
    public void fadeAndPausePausesWhenFadeFinishes() {
        playbackService.onCreate();

        playbackService.fadeAndPause();
        playbackService.onFadeFinished();

        verify(streamPlayer).pause();
    }

    @Test
    public void itDoesNotPauseWhenFadeFinishesAnNoPauseWasRequested() {
        playbackService.onCreate();

        playbackService.onFadeFinished();

        verify(streamPlayer, never()).pause();
    }

}
