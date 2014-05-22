package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.peripherals.PeripheralsOperations;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.track.TrackOperations;
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
import rx.Scheduler;
import rx.Subscription;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Iterator;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackServiceTest {

    PlaybackService playbackService;

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private EventBus eventBus;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private TrackOperations trackOperations;
    @Mock
    private PeripheralsOperations peripheralsOperations;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private StreamPlaya streamPlayer;
    @Mock
    private PlaybackReceiver.Factory playbackReceiverFactory;
    @Mock
    private PlaybackReceiver playbackReceiver;
    @Mock
    private Lazy<IRemoteAudioManager> audioManagerProvider;
    @Mock
    private IRemoteAudioManager remoteAudioManager;
    @Mock
    private PlayQueue playQueue;
    @Mock
    private Playa.StateTransition stateTransition;
    @Mock
    private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        playbackService = new PlaybackService(applicationProperties, playQueueManager, eventBus, trackOperations, peripheralsOperations,
                accountOperations, imageOperations, streamPlayer,
                playbackReceiverFactory, audioManagerProvider, featureFlags);
        when(playbackReceiverFactory.create(playbackService, accountOperations, playQueueManager, eventBus)).thenReturn(playbackReceiver);
        when(audioManagerProvider.get()).thenReturn(remoteAudioManager);
        when(playQueueManager.getCurrentPlayQueue()).thenReturn(playQueue);
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
    public void onCreateRegistersPlaybackReceiverToListenForReloadQueueAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RELOAD_QUEUE)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForPlayQueueChangedAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersPlaybackReceiverToListenForRetryRelatedTracksAction() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(PlaybackService.Actions.RETRY_RELATED_TRACKS)).toContain(playbackReceiver);
    }

    @Test
    public void onCreateRegistersNoisyListenerToListenForAudioBecomingNoisyBroadcast() throws Exception {
        playbackService.onCreate();
        expect(getReceiversForAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).toContain(playbackReceiver);
    }

    @Test
    public void onPlaystateChangedPublishesStateTransition() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        when(stateTransition.getNewState()).thenReturn(Playa.PlayaState.BUFFERING);
        when(stateTransition.trackEnded()).thenReturn(false);

        playbackService.onPlaystateChanged(stateTransition);

        Playa.StateTransition broadcasted = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        expect(broadcasted).toBe(stateTransition);
    }

    @Test
    public void onProgressDoesNotPublisheProgressEvent() throws Exception {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        playbackService.onProgressEvent(123L, 456L);

        eventMonitor.verifyNoEventsOn(EventQueue.PLAYBACK_PROGRESS);
    }

    @Test
    public void onProgressPublishesAProgressEventIfVisualPlayerEnabled() throws Exception {
        when(featureFlags.isEnabled(Feature.VISUAL_PLAYER)).thenReturn(true);
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        playbackService.onProgressEvent(123L, 456L);

        PlaybackProgressEvent broadcasted = eventMonitor.verifyEventOn(EventQueue.PLAYBACK_PROGRESS);
        expect(broadcasted.getProgress()).toEqual(123L);
        expect(broadcasted.getDuration()).toEqual(456L);
    }

    @Test
    public void stopSavesCurrentQueueAndPosition() throws Exception {
        when(streamPlayer.getProgress()).thenReturn(123L);
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());
        playbackService.openCurrent(new Track());
        playbackService.stop();
        verify(playQueueManager).saveCurrentPosition(123L);
    }

    @Test
    public void openCurrentLoadsStreamableTrackFromTrackOperations() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);
        final TestObservables.MockObservable<Track> trackMockObservable = TestObservables.emptyObservable();
        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(trackMockObservable);
        playbackService.openCurrent(new Track());
        expect(trackMockObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void openCurrentUnsubscribesPreviousLoadsStreamableTrackObservable() throws Exception {
        when(streamPlayer.getLastStateTransition()).thenReturn(Playa.StateTransition.DEFAULT);

        final Subscription subscription = Mockito.mock(Subscription.class);
        final Observable<Track> observable = TestObservables.fromSubscription(subscription);

        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(observable);

        playbackService.openCurrent(new Track());

        // we need to setup a different observable or we will get an undesired unsubscribe from the extra subscription
        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(TestObservables.<Track>emptyObservable());
        playbackService.openCurrent(new Track());

        verify(subscription).unsubscribe();
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

        when(streamPlayer.getLastStateTransition()).thenReturn(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));
        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());
        playbackService.openCurrent(new Track());

        playbackService.stop();
        playbackService.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE));

        ShadowService service = Robolectric.shadowOf(playbackService);
        expect(service.getLastForegroundNotification()).toBeNull();

    }

    @Test
    public void nonPauseStateCreatesNotificationAfterStoppingAndOpeningNewTrack() throws Exception {
        when(applicationProperties.shouldUseRichNotifications()).thenReturn(true);
        playbackService.onCreate();

        when(streamPlayer.getLastStateTransition()).thenReturn(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));
        when(trackOperations.loadStreamableTrack(anyLong(), any(Scheduler.class))).thenReturn(Observable.<Track>empty());
        playbackService.openCurrent(new Track());

        playbackService.stop();
        playbackService.openCurrent(new Track());
        playbackService.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));

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
}
