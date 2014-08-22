package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackSessionAnalyticsController;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowService;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Iterator;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackServiceTest {

    public static final int DURATION = 1000;
    private PlaybackService playbackService;
    private PropertySet track;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private ApplicationProperties applicationProperties;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackServiceOperations playbackServiceOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamPlaya streamPlayer;
    @Mock private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock private PlaybackReceiver playbackReceiver;
    @Mock private Lazy<IRemoteAudioManager> audioManagerProvider;
    @Mock private IRemoteAudioManager remoteAudioManager;
    @Mock private PlayQueue playQueue;
    @Mock private Playa.StateTransition stateTransition;
    @Mock private PlaybackNotificationController playbackNotificationController;
    @Mock private PlaybackSessionAnalyticsController analyticsController;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(playQueueManager, eventBus, trackOperations,
                accountOperations, playbackServiceOperations, streamPlayer,
                playbackReceiverFactory, audioManagerProvider, playbackNotificationController, analyticsController);

        track = TestPropertySets.expectedTrackForPlayer();

        when(playbackReceiverFactory.create(playbackService, accountOperations, playQueueManager, eventBus)).thenReturn(playbackReceiver);
        when(audioManagerProvider.get()).thenReturn(remoteAudioManager);
    }

    @Test
    public void onCreateSetsServiceAsListenerOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        verify(streamPlayer).setListener(playbackService);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForToggleplaybackAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPauseAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.PAUSE_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForSeek() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.SEEK)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForResetAllAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RESET_ALL)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForStopAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.STOP_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPlayQueueChangedAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).toContain(playbackReceiver);
    }

    @Test
    public void onCreatePublishedServiceLifecycleForCreated() throws Exception {
        playbackService.onCreate();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        expect(broadcasted.getKind()).toBe(PlayerLifeCycleEvent.STATE_CREATED);
    }

    @Test
    public void onDestroyPublishedServiceLifecycleForDestroyed() throws Exception {
        playbackService.onCreate();
        playbackService.onDestroy();

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        expect(broadcasted.getKind()).toBe(PlayerLifeCycleEvent.STATE_DESTROYED);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransition() throws Exception {
        playbackService.onCreate();

        when(stateTransition.getNewState()).thenReturn(Playa.PlayaState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);

        playbackService.onPlaystateChanged(stateTransition);

        Playa.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        expect(broadcasted).toBe(stateTransition);
    }

    @Test
    public void shouldForwardPlayerStateTransitionToAnalyticsController() {
        playbackService.onCreate();

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController).onStateTransition(stateTransition);
    }

    @Test
    public void onProgressPublishesAProgressEvent() throws Exception {
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(track));
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        playbackService.onCreate();
        playbackService.openCurrent(track, false);

        playbackService.onProgressEvent(123L, 456L);

        PlaybackProgressEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS);
        expect(broadcasted.getTrackUrn()).toEqual(getTrackUrn());
        expect(broadcasted.getPlaybackProgress().getPosition()).toEqual(123L);
        expect(broadcasted.getPlaybackProgress().getDuration()).toEqual(456L);
    }

    @Test
    public void openCurrentWithStreamableTrackCallsPlayOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(track));
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.<PropertySet>empty());

        playbackService.openCurrent(track, false);

        verify(streamPlayer).play(track);
    }

    @Test
    public void openCurrentWithAudioAdCallsPlayUninterruptedOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(trackOperations.track(any(TrackUrn.class))).thenReturn(Observable.just(track));

        playbackService.openCurrent();

        verify(streamPlayer).playUninterrupted(track);
    }

    @Test
    public void stopCallsStopOnStreamPlaya() throws Exception {
        playbackService.stop();
        verify(streamPlayer).stop();
    }

    @Test
    public void callingStopSuppressesIdleNotifications() throws Exception {
        when(applicationProperties.shouldUseRichNotifications()).thenReturn(true);
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, getTrackUrn()));
        playbackService.openCurrent(track, false);

        playbackService.stop();
        playbackService.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, getTrackUrn()));

        ShadowService service = Robolectric.shadowOf(playbackService);
        expect(service.getLastForegroundNotification()).toBeNull();

    }

    @Test
    public void nonPauseStateCreatesNotificationAfterStoppingAndOpeningNewTrack() throws Exception {
        when(applicationProperties.shouldUseRichNotifications()).thenReturn(true);
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, getTrackUrn()));
        when(playbackNotificationController.playingNotification()).thenReturn(Observable.just(Mockito.mock(Notification.class)));
        playbackService.openCurrent(track, false);

        final TrackUrn trackUrn2 = Urn.forTrack(456);
        final PropertySet track2 = TestPropertySets.expectedTrackForPlayer().put(TrackProperty.URN, trackUrn2);
        playbackService.stop();
        playbackService.openCurrent(track2, false);
        playbackService.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn2));

        ShadowService service = Robolectric.shadowOf(playbackService);
        final Notification lastForegroundNotification = service.getLastForegroundNotification();
        expect(lastForegroundNotification).not.toBeNull();
    }

    private ArrayList<BroadcastReceiver> getReceiversForAction(String action) {
        ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<BroadcastReceiver>();
        for (ShadowApplication.Wrapper registeredReceiver : Robolectric.getShadowApplication().getRegisteredReceivers()) {
            if (registeredReceiver.context == playbackService) {
                Iterator<String> actions = registeredReceiver.intentFilter.actionsIterator();
                while (actions.hasNext()) {
                    if (actions.next().equals(action)) {
                        broadcastReceivers.add(registeredReceiver.broadcastReceiver);
                    }
                }
            }

        }
        return broadcastReceivers;
    }
    
    private TrackUrn getTrackUrn() {
        return track.get(TrackProperty.URN);
    }
}
