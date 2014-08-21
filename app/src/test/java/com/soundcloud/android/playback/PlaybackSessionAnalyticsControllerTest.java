package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionAnalyticsControllerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(1L);
    private static final UserUrn USER_URN = Urn.forUser(2L);
    private static final long PROGRESS = 123L;
    private static final int DURATION = 456;

    private PlaybackSessionAnalyticsController playbackSessionAnalyticsController;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private TrackOperations trackOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        PropertySet track = TestPropertySets.expectedTrackForAnalytics(TRACK_URN, "allow", DURATION);
        when(trackOperations.track(TRACK_URN)).thenReturn(Observable.just(track));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);

        playbackSessionAnalyticsController = new PlaybackSessionAnalyticsController(
                eventBus, trackOperations, accountOperations, playQueueManager);
        playbackSessionAnalyticsController.subscribe();
    }

    @Test
    public void playQueueChangedEventDoesNotPublishEventWithNoActiveSession() throws Exception {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void positionChangedEventDoesNotPublishEventWithNoActiveSession() throws Exception {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void stateChangeEventDoesNotPublishEventWithInvalidTrackUrn() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TrackUrn.NOT_SET));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() throws Exception {
        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = eventBus.firstEventOn(EventQueue.PLAYBACK_SESSION);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
    }

    private void expectCommonAudioEventData(Playa.StateTransition playEvent, PlaybackSessionEvent playbackSessionEvent) {
        expect(playbackSessionEvent.getTrackUrn()).toEqual(TRACK_URN);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
        expect(playbackSessionEvent.getUserUrn()).toEqual(USER_URN);
        expect(playbackSessionEvent.getProgress()).toEqual(PROGRESS);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackSessionEvent.getProtocol()).toEqual(playEvent.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL));
    }

    @Test
    public void stateChangeEventForPlayingAudioAdPublishesAdSpecificPlayEvent() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(audioAd);

        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = eventBus.firstEventOn(EventQueue.PLAYBACK_SESSION);
        // track properties
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        // ad specific properties
        expect(playbackSessionEvent.getAudioAdUrn()).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(playbackSessionEvent.getAudioAdMonetizedUrn()).toEqual(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForBuffering() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_BUFFERING);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForPause() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.NONE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_PAUSE);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForTrackFinished() throws Exception {
        publishPlayingEvent();
        when(playQueueManager.hasNextTrack()).thenReturn(true);
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForQueueFinished() throws Exception {
        publishPlayingEvent();
        when(playQueueManager.hasNextTrack()).thenReturn(false);
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE);
    }

    @Test
    public void stateChangeEventInNonPlayingStatePublishesStopEventForError() throws Exception {
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED);

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_ERROR);
    }

    @Test
    public void playQueueEventForNewQueuePublishesStopEventForNewQueue() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_NEW_QUEUE);
    }

    @Test
    public void playQueueEventFromPositionChangedPublishesStopEventForSkip() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Test
    public void playQueueEventFromPositionChangePublishesStopEventForSkipWithPreviousDuration() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    protected Playa.StateTransition publishPlayingEvent() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        final Playa.StateTransition startEvent = new Playa.StateTransition(
                Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN, PROGRESS, DURATION);
        startEvent.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, startEvent);

        return startEvent;
    }

    protected void publishStopEvent(Playa.PlayaState newState, Playa.Reason reason) {
        final Playa.StateTransition stopEvent = new Playa.StateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stopEvent);
    }

    protected void verifyStopEvent(int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent = eventBus.lastEventOn(EventQueue.PLAYBACK_SESSION);
        expect(playbackSessionEvent.getTrackUrn()).toEqual(TRACK_URN);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isStopEvent()).toBeTrue();
        expect(playbackSessionEvent.getUserUrn()).toEqual(USER_URN);
        expect(playbackSessionEvent.getProgress()).toEqual(PROGRESS);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackSessionEvent.getStopReason()).toEqual(stopReason);
        expect(playbackSessionEvent.getDuration()).toEqual(DURATION);
    }
}
