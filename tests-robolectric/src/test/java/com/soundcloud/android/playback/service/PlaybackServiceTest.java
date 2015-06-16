package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
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
import com.soundcloud.android.playback.PlaybackSessionAnalyticsController;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.BroadcastReceiver;
import android.content.Intent;
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
    @Mock private TrackRepository trackRepository;
    @Mock private AccountOperations accountOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private StreamPlaya streamPlayer;
    @Mock private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock private PlaybackReceiver playbackReceiver;
    @Mock private IRemoteAudioManager remoteAudioManager;
    @Mock private PlayQueue playQueue;
    @Mock private Playa.StateTransition stateTransition;
    @Mock private PlaybackNotificationController playbackNotificationController;
    @Mock private PlaybackSessionAnalyticsController analyticsController;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(playQueueManager, eventBus, trackRepository,
                accountOperations, streamPlayer,
                playbackReceiverFactory, InjectionSupport.lazyOf(remoteAudioManager), playbackNotificationController, analyticsController, adsOperations);

        track = TestPropertySets.expectedTrackForPlayer();

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
    public void onStartPublishesServiceLifecycleForStarted() throws Exception {
        playbackService.onCreate();
        playbackService.stop();
        playbackService.onStartCommand(new Intent(), 0, 0);

        PlayerLifeCycleEvent broadcasted = eventBus.lastEventOn(EventQueue.PLAYER_LIFE_CYCLE);
        expect(broadcasted.getKind()).toBe(PlayerLifeCycleEvent.STATE_STARTED);
    }

    @Test
    public void onStartWithNullIntentStopsSelf() throws Exception {
        playbackService.onCreate();
        playbackService.onStartCommand(null, 0, 0);
        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        ShadowService service = Robolectric.shadowOf(playbackService);
        expect(service.isStoppedBySelf()).toBeTrue();
    }

    @Test
    public void onPlaystateChangedPublishesStateTransition() throws Exception {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(track));
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(stateTransition.getNewState()).thenReturn(Playa.PlayaState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);
        when(stateTransition.getTrackUrn()).thenReturn(getTrackUrn());

        playbackService.onCreate();
        playbackService.openCurrent(track, false);

        playbackService.onPlaystateChanged(stateTransition);

        Playa.StateTransition broadcasted = eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        expect(broadcasted).toBe(stateTransition);
    }

    @Test
    public void onPlaystateChangedDoesNotPublishStateTransitionWithDifferentUrnThanCurrent() {
        when(stateTransition.getTrackUrn()).thenReturn(getTrackUrn());

        playbackService.onPlaystateChanged(stateTransition);

        expect(eventBus.eventsOn(EventQueue.PLAYBACK_STATE_CHANGED)).toBeEmpty();
    }

    @Test
    public void shouldForwardPlayerStateTransitionToAnalyticsController() {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(track));
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(stateTransition.getNewState()).thenReturn(Playa.PlayaState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);
        when(stateTransition.getTrackUrn()).thenReturn(getTrackUrn());

        playbackService.onCreate();
        playbackService.openCurrent(track, false);

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController).onStateTransition(stateTransition);
    }

    @Test
    public void doesNotForwardPlayerStateTransitionToAnalyticsControllerWithDifferentUrnThenCurrent() {
        when(stateTransition.getTrackUrn()).thenReturn(getTrackUrn());

        playbackService.onPlaystateChanged(stateTransition);

        verify(analyticsController, never()).onStateTransition(any(Playa.StateTransition.class));
    }

    @Test
    public void onProgressPublishesAProgressEvent() throws Exception {
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(track));
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
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(track));
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.<PropertySet>empty());

        playbackService.openCurrent(track, false);

        verify(streamPlayer).play(track);
    }

    @Test
    public void openCurrentWithAudioAdCallsPlayUninterruptedOnStreamPlayer() throws Exception {
        playbackService.onCreate();
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.isQueueEmpty()).thenReturn(false);
        when(trackRepository.track(any(Urn.class))).thenReturn(Observable.just(track));

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
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, getTrackUrn()));
        playbackService.openCurrent(track, false);

        playbackService.stop();
        playbackService.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, getTrackUrn()));

        ShadowService service = Robolectric.shadowOf(playbackService);
        expect(service.getLastForegroundNotification()).toBeNull();

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
    
    private Urn getTrackUrn() {
        return track.get(TrackProperty.URN);
    }
}
