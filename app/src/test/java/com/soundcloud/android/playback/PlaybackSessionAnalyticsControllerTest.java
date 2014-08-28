package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdProperty;
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

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackSessionAnalyticsControllerTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(1L);
    private static final UserUrn USER_URN = Urn.forUser(2L);
    private static final long PROGRESS = 123L;
    private static final int DURATION = 456;

    private PlaybackSessionAnalyticsController analyticsController;
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

        analyticsController = new PlaybackSessionAnalyticsController(
                eventBus, trackOperations, accountOperations, playQueueManager);
    }

    @Test
    public void stateChangeEventDoesNotPublishEventWithInvalidTrackUrn() throws Exception {
        analyticsController.onStateTransition(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TrackUrn.NOT_SET));
        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_SESSION);
    }

    @Test
    public void stateChangeEventWithValidTrackUrnInPlayingStatePublishesPlayEvent() throws Exception {
        Playa.StateTransition playEvent = publishPlayingEvent();

        PlaybackSessionEvent playbackSessionEvent = eventBus.firstEventOn(EventQueue.PLAYBACK_SESSION);
        expectCommonAudioEventData(playEvent, playbackSessionEvent);
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
    }

    private void expectCommonAudioEventData(Playa.StateTransition stateTransition, PlaybackSessionEvent playbackSessionEvent) {
        expect(playbackSessionEvent.getTrackUrn()).toEqual(TRACK_URN);
        expect(playbackSessionEvent.getTrackSourceInfo()).toBe(trackSourceInfo);
        expect(playbackSessionEvent.getUserUrn()).toEqual(USER_URN);
        expect(playbackSessionEvent.getProgress()).toEqual(PROGRESS);
        expect(playbackSessionEvent.getTimeStamp()).toBeGreaterThan(0L);
        expect(playbackSessionEvent.getProtocol()).toEqual(stateTransition.getExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL));
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
        expect(playbackSessionEvent.isStopEvent()).toBeFalse();
        // ad specific properties
        expect(playbackSessionEvent.getAudioAdUrn()).toEqual(audioAd.get(AdProperty.AD_URN));
        expect(playbackSessionEvent.getAudioAdMonetizedUrn()).toEqual(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
    }

    @Test
    public void stateChangeEventForFinishPlayingAudioAdPublishesAdSpecificStopEvent() throws Exception {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(audioAd);
        when(playQueueManager.hasNextTrack()).thenReturn(true);

        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.BUFFERING, Playa.Reason.NONE); // make sure intermediate events don't matter
        publishPlayingEvent();
        publishStopEvent(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE);

        PlaybackSessionEvent playbackSessionEvent = eventBus.lastEventOn(EventQueue.PLAYBACK_SESSION);
        verifyStopEvent(PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED);
        expect(playbackSessionEvent.hasTrackFinished()).toBeTrue();
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

    // the player does not send stop events when skipping, so we have to materialize these
    @Test
    public void shouldPublishStopEventWhenUserSkipsBetweenTracksManually() {
        final TrackUrn nextTrack = Urn.forTrack(456L);
        when(trackOperations.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        publishPlayingEventForTrack(TRACK_URN);
        publishPlayingEventForTrack(nextTrack);

        List<PlaybackSessionEvent> events = eventBus.eventsOn(EventQueue.PLAYBACK_SESSION);
        expect(events).toNumber(3);
        expect(events.get(1).isStopEvent()).toBeTrue();
        expect(events.get(1).getTrackUrn()).toEqual(TRACK_URN);
    }

    @Test
    public void shouldPublishStopEventWithAdDataWhenUserSkipsBetweenTracksManually() {
        PropertySet audioAd = TestPropertySets.audioAdProperties(TRACK_URN);
        final TrackUrn nextTrack = Urn.forTrack(456L);
        when(trackOperations.track(nextTrack)).thenReturn(Observable.just(TestPropertySets.expectedTrackForAnalytics(nextTrack)));

        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(audioAd);

        publishPlayingEventForTrack(TRACK_URN);

        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);
        publishPlayingEventForTrack(nextTrack);

        List<PlaybackSessionEvent> events = eventBus.eventsOn(EventQueue.PLAYBACK_SESSION);
        expect(events).toNumber(3);
        expect(events.get(1).isStopEvent()).toBeTrue();
        expect(events.get(1).getTrackUrn()).toEqual(TRACK_URN);
        expect(events.get(1).getAudioAdUrn()).toEqual(audioAd.get(AdProperty.AD_URN));
    }

    protected Playa.StateTransition publishPlayingEvent() {
        return publishPlayingEventForTrack(TRACK_URN);
    }

    protected Playa.StateTransition publishPlayingEventForTrack(TrackUrn trackUrn) {
        final Playa.StateTransition startEvent = new Playa.StateTransition(
                Playa.PlayaState.PLAYING, Playa.Reason.NONE, trackUrn, PROGRESS, DURATION);
        startEvent.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");

        analyticsController.onStateTransition(startEvent);

        return startEvent;
    }

    protected Playa.StateTransition publishStopEvent(Playa.PlayaState newState, Playa.Reason reason) {
        final Playa.StateTransition stopEvent = new Playa.StateTransition(
                newState, reason, TRACK_URN, PROGRESS, DURATION);
        stopEvent.addExtraAttribute(Playa.StateTransition.EXTRA_PLAYBACK_PROTOCOL, "hls");
        analyticsController.onStateTransition(stopEvent);
        return stopEvent;
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
