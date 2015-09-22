package com.soundcloud.android.playback;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.media.AudioManager;

public class PlaybackServiceTest extends AndroidUnitTest {

    private static final PropertySet TRACK_PROPERTIES = TestPropertySets.expectedTrackForPlayer();
    private PlaybackService playbackService;
    private Urn track = Urn.forTrack(123L);
    private TestEventBus eventBus = new TestEventBus();

    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackRepository trackRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamPlayer streamPlayer;
    @Mock private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock private PlaybackReceiver playbackReceiver;
    @Mock private IRemoteAudioManager remoteAudioManager;
    @Mock private PlayQueue playQueue;
    @Mock private Player.StateTransition stateTransition;
    @Mock private PlaybackNotificationController playbackNotificationController;
    @Mock private PlaybackSessionAnalyticsController analyticsController;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(playQueueManager, eventBus, trackRepository,
                accountOperations, streamPlayer,
                playbackReceiverFactory, InjectionSupport.lazyOf(remoteAudioManager), playbackNotificationController, analyticsController, adsOperations);

        when(trackRepository.track(track)).thenReturn(Observable.just(TRACK_PROPERTIES));

        when(playbackReceiverFactory.create(playbackService, accountOperations, playQueueManager, eventBus)).thenReturn(playbackReceiver);
    }

    @Test
    public void onCreateSetsServiceAsListenerOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        verify(streamPlayer).setListener(playbackService);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForToggleplaybackAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.TOGGLE_PLAYBACK);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPauseAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.PAUSE);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForSeek() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.SEEK);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForResetAllAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.RESET_ALL);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForStopAction() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, PlaybackService.Action.STOP);
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        assertThat(playbackService).hasRegisteredReceiverWithAction(playbackReceiver, AudioManager.ACTION_AUDIO_BECOMING_NOISY);
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
    public void onStartWillSubscribePlaybackNotification() {
        playbackService.onStartCommand(null, 0, 0);

        verify(playbackNotificationController).subscribe(playbackService);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransition() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        when(stateTransition.getNewState()).thenReturn(Player.PlayerState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);
        when(stateTransition.getTrackUrn()).thenReturn(track);

        playbackService.onCreate();
        playbackService.play(track, 0);

        playbackService.onPlaystateChanged(stateTransition);

        Player.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        assertThat(broadcasted).isEqualTo(stateTransition);
    }

    @Test
    public void onPlaystateChangedDoesNotPublishStateTransitionWithDifferentUrnThanCurrent() {
        when(stateTransition.getTrackUrn()).thenReturn(track);

        playbackService.onPlaystateChanged(stateTransition);

        assertThat(eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED)).isEmpty();
    }

    @Test
    public void shouldForwardPlayerStateTransitionToAnalyticsController() {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        when(stateTransition.getNewState()).thenReturn(Player.PlayerState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);
        when(stateTransition.getTrackUrn()).thenReturn(track);

        playbackService.onCreate();
        playbackService.play(track, 0);

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController).onStateTransition(stateTransition);
    }

    @Test
    public void doesNotForwardPlayerStateTransitionToAnalyticsControllerWithDifferentUrnThenCurrent() {
        when(stateTransition.getTrackUrn()).thenReturn(track);

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController, never()).onStateTransition(any(Player.StateTransition.class));
    }

    @Test
    public void onProgressPublishesAProgressEvent() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        playbackService.onCreate();
        playbackService.play(track, 0);

        playbackService.onProgressEvent(123L, 456L);

        PlaybackProgressEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS);
        assertThat(broadcasted.getTrackUrn()).isEqualTo(track);
        assertThat(broadcasted.getPlaybackProgress().getPosition()).isEqualTo(123L);
        assertThat(broadcasted.getPlaybackProgress().getDuration()).isEqualTo(456L);
    }

    @Test
    public void openCurrentWithStreamableTrackCallsPlayOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);

        playbackService.play(track, 0);

        verify(streamPlayer).play(track, 0, TRACK_PROPERTIES.get(TrackProperty.DURATION));
    }

    @Test
    public void openCurrentWithAudioAdCallsPlayUninterruptedOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Player.StateTransition.DEFAULT);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);

        playbackService.playUninterrupted(track);

        verify(streamPlayer).playUninterrupted(track, TRACK_PROPERTIES.get(TrackProperty.DURATION));
    }

    @Test
    public void stopCallsStopOnStreamPlaya() throws Exception {
        playbackService.stop();
        verify(streamPlayer).stop();
    }

    @Test
    public void callingStopSuppressesIdleNotifications() throws Exception {
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, track));
        playbackService.play(track, 0);

        playbackService.stop();
        playbackService.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.NONE, track));

        assertThat(playbackService).doesNotHaveLastForegroundNotification();
    }
}
