package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.track.LegacyTrackOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionAnalyticsControllerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(1L);
    private static final UserUrn USER_URN = Urn.forUser(2L);

    private PlaybackSessionAnalyticsController playbackSessionAnalyticsController;
    private TestEventBus eventBus = new TestEventBus();
    private Track track;

    @Mock
    private LegacyTrackOperations trackOperations;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        playbackSessionAnalyticsController = new PlaybackSessionAnalyticsController(eventBus, trackOperations, accountOperations, playQueueManager);
        playbackSessionAnalyticsController.subscribe();
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);

        track = TestHelper.getModelFactory().createModel(Track.class);
        track.duration = 1000;
        when(trackOperations.loadTrack(anyLong(), same(AndroidSchedulers.mainThread()))).thenReturn(Observable.just(track));
    }

    @Test
    public void playQueueChangedEventDoesNotPublishEventWithNoActiveSession() throws Exception {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void trackChangedEventDoesNotPublishEventWithNoActiveSession() throws Exception {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void stateChangeEventDoesNotPublishEventWithInvalidTrackUrn() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TrackUrn.NOT_SET));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() throws Exception {
        publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = eventBus.firstEventOn(EventQueue.PLAYBACK_SESSION);
        expect(playbackSessionEvent.getTrackUrn()).toEqual(TRACK_URN);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
        expect(playbackSessionEvent.getUserUrn()).toEqual(USER_URN);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
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
    public void playQueueEventForQueueChangePublishesStopEventForNewQueue() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_NEW_QUEUE);
    }

    @Test
    public void playQueueEventForTrackChangePublishesStopEventForSkip() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Test
    public void playQueueEventForTrackChangePublishesStopEventForSkipWithPreviousDuration() throws Exception {
        publishPlayingEvent();

        track.duration = 2000;
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange(track.getUrn()));

        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_SKIP);
    }

    @Test
    public void playQueueEventForQueueUpdateDoesNotSendStopEvent() throws Exception {
        publishPlayingEvent();

        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(track.getUrn()));

        expect(eventBus.lastEventOn(EventQueue.PLAYBACK_SESSION).isPlayEvent()).toBeTrue();
    }

    protected void publishPlayingEvent() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(track.getUrn()));

        final Playa.StateTransition startEvent = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, startEvent);
    }

    protected void publishStopEvent(Playa.PlayaState newState, Playa.Reason reason) {
        final Playa.StateTransition stopEvent = new Playa.StateTransition(newState, reason, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stopEvent);
    }

    protected void verifyStopEvent(int stopReason) {
        final PlaybackSessionEvent playbackSessionEvent = eventBus.lastEventOn(EventQueue.PLAYBACK_SESSION);
        expect(playbackSessionEvent.getTrackUrn()).toEqual(TRACK_URN);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.isStopEvent()).toBeTrue();
        expect(playbackSessionEvent.getUserUrn()).toEqual(USER_URN);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackSessionEvent.getStopReason()).toEqual(stopReason);
        expect(playbackSessionEvent.getDuration()).toEqual(1000L);
    }
}
